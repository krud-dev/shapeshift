/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.resolver.annotation

import dev.krud.shapeshift.ShapeShiftBuilder
import dev.krud.shapeshift.decorator.Decorator
import dev.krud.shapeshift.dto.ResolvedMappedField
import dev.krud.shapeshift.dto.TransformerCoordinates
import dev.krud.shapeshift.resolver.MappingResolver
import dev.krud.shapeshift.resolver.MappingResolverResolution
import dev.krud.shapeshift.util.getDeclaredFieldRecursive
import dev.krud.shapeshift.util.splitIgnoreEmpty
import java.lang.reflect.Field
import kotlin.reflect.full.createInstance

class ShapeShiftAnnotationMappingResolver : MappingResolver {
    override fun resolve(sourceClazz: Class<*>, targetClazz: Class<*>): MappingResolverResolution {
        val defaultMappingTarget = sourceClazz.getDeclaredAnnotation(DefaultMappingTarget::class.java)
        val defaultToClass: Class<*> = defaultMappingTarget?.value?.java ?: Nothing::class.java
        val mappedFieldReferences = getMappedFields(sourceClazz, targetClazz, defaultToClass)

        val resolvedMappedFields = mutableListOf<ResolvedMappedField>()
        for ((mappedField, field) in mappedFieldReferences) {

            val transformerCoordinates = if (mappedField.transformerRef.isBlank()) {
                TransformerCoordinates.ofType(mappedField.transformer.java)
            } else {
                TransformerCoordinates.ofName(mappedField.transformerRef)
            }

            val mapFromCoordinates = resolveNodesToFields(mappedField.mapFrom.splitIgnoreEmpty(NODE_DELIMITER), field, sourceClazz)

            val effectiveMapTo = mappedField.mapTo.ifBlank {
                mapFromCoordinates.last().name
            }

            val mapToCoordinates = resolveNodesToFields(effectiveMapTo.splitIgnoreEmpty(NODE_DELIMITER), null, targetClazz)
            val conditionClazz = if(mappedField.condition != Nothing::class) {
                mappedField.condition
            } else {
                null
            }

            resolvedMappedFields += ResolvedMappedField(
                mapFromCoordinates,
                mapToCoordinates,
                transformerCoordinates,
                null,
                conditionClazz?.java,
                null,
                mappedField.overrideMappingStrategy
            )
        }

        val decorators = sourceClazz.getDeclaredAnnotationsByType(Decorate::class.java)
            .filter { it.target == targetClazz || (defaultToClass != Nothing::class.java && it.target == Nothing::class && defaultToClass == targetClazz) }
            .map { it.decorator.createInstance() }
        return MappingResolverResolution(resolvedMappedFields, decorators)
    }

    /**
     * Get true from field from path like "user.address.city" delimtied by [NODE_DELIMITER]
     */
    fun resolveNodesToFields(nodes: List<String>, field: Field?, clazz: Class<*>): List<Field> {
        if (field == null) {
            if (nodes.isEmpty()) {
                error("Unable to determine mapped field for $clazz")
            }
            val realField = clazz.getDeclaredFieldRecursive(nodes.first())
            return resolveNodesToFields(nodes.drop(1), realField, realField.type)
        }

        if (nodes.isEmpty()) {
            return listOf(field)
        }
        val nextField = field.type.getDeclaredFieldRecursive(nodes.first())
        return listOf(field) + resolveNodesToFields(nodes.drop(1), nextField, nextField.type)
    }

    private fun getMappedFields(fromClass: Class<*>, toClass: Class<*>, defaultToClass: Class<*>): List<MappedFieldReference> {
        var clazz: Class<*>? = fromClass
        val result = mutableListOf<MappedFieldReference>()
        while (clazz != null) {
            val fields = clazz.declaredFields
            result += clazz.getDeclaredAnnotationsByType(MappedField::class.java)
                .filter { mappedField ->
                    try {
                        return@filter isOfType(defaultToClass, mappedField.target.java, toClass)
                    } catch (e: IllegalStateException) {
                        error("Could not create entity structure for <" + fromClass.simpleName + ", " + toClass.simpleName + ">: " + e.message)
                    }
                }
                .map { MappedFieldReference(it) }
            for (field in fields) {
                result += field.getDeclaredAnnotationsByType(MappedField::class.java)
                    .filter { mappedField ->
                        try {
                            return@filter isOfType(defaultToClass, mappedField.target.java, toClass)
                        } catch (e: IllegalStateException) {
                            throw IllegalStateException("Could not create entity structure for <" + fromClass.simpleName + ", " + toClass.simpleName + ">: " + e.message)
                        }
                    }
                    .map { MappedFieldReference(it, field) }
            }
            clazz = clazz.superclass
        }

        return result
    }

    private fun isOfType(defaultToClass: Class<*>, fromClass: Class<*>, toClass: Class<*>): Boolean {
        var trueFromClass: Class<*> = fromClass
        if (trueFromClass == Nothing::class.java) {
            check(defaultToClass != Nothing::class.java) { "No mapping target or default mapping target specified" }
            trueFromClass = defaultToClass
        }
        return trueFromClass.isAssignableFrom(toClass)
    }

    private data class MappedFieldReference(
        val mappedField: MappedField,
        val field: Field? = null
    )

    companion object {
        private const val NODE_DELIMITER = "."
    }
}

@DefaultMappingTarget(ExampleB::class)
@Decorate(decorator = MyDec::class)
class ExampleA {
    var a = "aaa"
}

class ExampleB {
    var b = "bbb"
    override fun toString(): String {
        return "ExampleB(b='$b')"
    }

}

class MyDec : Decorator<ExampleA, ExampleB> {
    override fun decorate(from: ExampleA, to: ExampleB) {
        to.b = "xxx"
    }
}

fun main() {
    val shapeShift = ShapeShiftBuilder()
        .withMappingResolver(ShapeShiftAnnotationMappingResolver())
        .build()
    val b: ExampleB = shapeShift.map(ExampleA())
    println(b)
}