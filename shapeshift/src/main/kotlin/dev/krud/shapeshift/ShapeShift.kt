/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift

import dev.krud.shapeshift.annotation.DefaultMappingTarget
import dev.krud.shapeshift.annotation.MappedField
import dev.krud.shapeshift.dto.MappingStructure
import dev.krud.shapeshift.dto.ObjectFieldTrio
import dev.krud.shapeshift.transformer.EmptyTransformer
import dev.krud.shapeshift.transformer.base.ClassPair
import dev.krud.shapeshift.transformer.base.FieldTransformer
import dev.krud.shapeshift.transformer.base.FieldTransformer.Companion.id
import dev.krud.shapeshift.util.getValue
import dev.krud.shapeshift.util.setValue
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

class ShapeShift constructor(
    transformersRegistrations: List<TransformerRegistration<out Any, out Any>> = emptyList()
) {
    internal val transformers: MutableList<TransformerRegistration<out Any, out Any>> = mutableListOf()
    internal val transformersByNameCache: MutableMap<String, TransformerRegistration<out Any, out Any>> = mutableMapOf()
    internal val transformersByTypeCache: MutableMap<Class<out FieldTransformer<*, *>>, TransformerRegistration<*, *>> =
        mutableMapOf()
    internal val defaultTransformers: MutableMap<ClassPair, TransformerRegistration<out Any, out Any>> = mutableMapOf()
    private val mappingStructures: MutableMap<ClassPair, MappingStructure> = mutableMapOf()
    private val entityFieldsCache: MutableMap<Class<*>, Map<String, Field>> = mutableMapOf()

    init {
        for (registration in transformersRegistrations) {
            registerTransformer(registration)
        }
    }

    fun <From : Any, To : Any> map(fromObject: From, toClazz: Class<To>): To {
        val mappingStructure = getMappingStructure(fromObject::class.java, toClazz)
        val toObject = toClazz.newInstance()

        for (typeAnnotation in mappingStructure.typeAnnotations) {
            if (typeAnnotation.mapFrom.isBlank()) {
                error("mapFrom can not be empty when used at a type level")
            }
            var trueFromPath = typeAnnotation.mapFrom

            if (trueFromPath.startsWith(fromObject::class.java.simpleName, ignoreCase = true)) {
                trueFromPath = trueFromPath.substring(trueFromPath.indexOf(NODE_DELIMITER) + 1)
            }

            var trueToPath = typeAnnotation.mapTo
            if (trueToPath.startsWith(toClazz.simpleName, ignoreCase = true)) {
                trueToPath = trueToPath.substring(trueToPath.indexOf(NODE_DELIMITER) + 1)
            }
            processMappedField(typeAnnotation, fromObject, toObject, trueFromPath, trueToPath)
        }

        for ((field, annotations) in mappingStructure.annotations) {
            for (annotation in annotations) {
                val trueFromPath = if (annotation.mapFrom.isBlank()) {
                    field.name
                } else {
                    if (!annotation.mapFrom.startsWith(field.name + NODE_DELIMITER)) {
                        field.name + NODE_DELIMITER + annotation.mapFrom
                    } else {
                        annotation.mapFrom
                    }
                }

                var trueToPath = annotation.mapTo
                if (trueToPath.startsWith(toClazz.simpleName, ignoreCase = true)) {
                    trueToPath = trueToPath.substring(trueToPath.indexOf(NODE_DELIMITER) + 1)
                }
                processMappedField(annotation, fromObject, toObject, trueFromPath, trueToPath)
            }
        }

        return toObject
    }

    private fun <From : Any, To : Any> processMappedField(
        annotation: MappedField,
        fromObject: From,
        toObject: To,
        fromPath: String,
        toPath: String
    ) {
        val fromPair = getFieldByPath(fromPath, fromObject, SourceType.FROM) ?: return
        val appliedToPath = toPath.ifBlank { fromPair.field.name }
        val toPair = getFieldByPath(appliedToPath, toObject, SourceType.TO) ?: return
        val transformerRegistration = getTransformer(annotation, fromPair, toPair)
        mapField(fromPair, toPair, transformerRegistration)
    }

    private fun mapField(fromPair: ObjectFieldTrio, toPair: ObjectFieldTrio, transformerRegistration: TransformerRegistration<*, *>) {
        fromPair.field.isAccessible = true
        toPair.field.isAccessible = true
        var value = fromPair.field.getValue(fromPair.target)
        if (transformerRegistration != TransformerRegistration.EMPTY) {
            val transformer = transformerRegistration.transformer as FieldTransformer<Any, Any>
            value = transformer.transform(fromPair.field, toPair.field, value, fromPair.target, toPair.target)
        }
        if (value != null) {
            try {
                if (!toPair.type.isAssignableFrom(value::class.java)) {
                    error("Type mismatch: Expected ${toPair.type} but got ${value::class.java}")
                }
                toPair.field.setValue(toPair.target, value)
            } catch (e: Exception) {
                val newException =
                    IllegalStateException("Could not map value ${fromPair.field.name} of class ${fromPair.target.javaClass.simpleName} to ${toPair.field.name} of class ${toPair.target.javaClass.simpleName}: ${e.message}")
                newException.initCause(e)
                throw newException
            }
        }
    }

    private fun getFieldByPath(path: String, target: Any?, type: SourceType): ObjectFieldTrio? {
        // This if only applies to recursive runs of this function
        // When target is null and type is from, don't attempt to instantiate the object
        if (target == null) {
            if (type == SourceType.FROM) {
                return null
            } else {
                // Impossible to reach
                error("$path leads to a null target")
            }
        }

        val nodes = path.split(NODE_DELIMITER_REGEX).toMutableList()

        val field = getField(nodes.first(), target::class.java) ?: error(
            "Field ${nodes.firstOrNull()} not found on class ${target::class.java}"
        )

        val fieldType = field.type.kotlin.javaObjectType

        if (nodes.size == 1) {
            return ObjectFieldTrio(target, field, fieldType)
        }
        nodes.removeFirst()
        field.isAccessible = true
        var subTarget = field.get(target)

        if (subTarget == null && type == SourceType.TO) {
            subTarget = fieldType.newInstance()
            field.set(target, subTarget)
        }

        return getFieldByPath(nodes.joinToString(NODE_DELIMITER), subTarget, type)
    }

    private fun getFieldsMap(clazz: Class<*>): Map<String, Field> {
        val existingInCache = entityFieldsCache[clazz]
        if (existingInCache != null) {
            return existingInCache
        }

        val fieldsMap = mutableMapOf<String, Field>()
        var classToGetFields: Class<*>? = clazz
        while (classToGetFields != null) {
            val fields = classToGetFields.declaredFields
            for (field in fields) {
                fieldsMap[field.name] = field
            }
            classToGetFields = classToGetFields.superclass
        }
        entityFieldsCache[clazz] = fieldsMap
        return fieldsMap
    }

    private fun getField(name: String, clazz: Class<*>): Field? {
        return getFieldsMap(clazz)[name]
    }

    private fun getTransformerByName(name: String): TransformerRegistration<out Any, out Any> {
        return transformersByNameCache.computeIfAbsent(name) { _ ->
            transformers.find { it.name == name } ?: TransformerRegistration.EMPTY
        }
    }

    private fun getTransformerByType(type: Class<out FieldTransformer<*, *>>): TransformerRegistration<out Any, out Any> {
        return transformersByTypeCache.computeIfAbsent(type) { _ ->
            transformers.find { it.transformer::class.java == type } ?: TransformerRegistration.EMPTY
        }
    }

    private fun getMappingStructure(fromClass: Class<*>, toClass: Class<*>): MappingStructure {
        val key = fromClass to toClass
        return mappingStructures.computeIfAbsent(key) {
            val annotations: MutableMap<Field, List<MappedField>> = HashMap()
            val typeAnnotations: MutableList<MappedField> = ArrayList()
            var clazz: Class<*>? = fromClass
            while (clazz != null) {
                val fields = clazz.declaredFields
                val defaultMappingTarget = clazz.getDeclaredAnnotation(DefaultMappingTarget::class.java)
                val defaultFromClass: Class<*> = defaultMappingTarget?.value?.java ?: Nothing::class.java
                typeAnnotations.addAll(
                    clazz.getDeclaredAnnotationsByType(MappedField::class.java)
                        .filter { mappedField ->
                            try {
                                return@filter isOfType(defaultFromClass, mappedField.target.java, toClass)
                            } catch (e: IllegalStateException) {
                                error("Could not create entity structure for <" + fromClass.simpleName + ", " + toClass.simpleName + ">: " + e.message)
                            }
                        }
                )
                for (field in fields) {
                    val availableAnnotations = field.getDeclaredAnnotationsByType(MappedField::class.java)
                        .filter { mappedField ->
                            try {
                                return@filter isOfType(defaultFromClass, mappedField.target.java, toClass)
                            } catch (e: IllegalStateException) {
                                throw IllegalStateException("Could not create entity structure for <" + fromClass.simpleName + ", " + toClass.simpleName + ">: " + e.message)
                            }
                        }
                    annotations[field] = availableAnnotations
                }
                clazz = clazz.superclass
            }
            MappingStructure(typeAnnotations, annotations)
        }
    }

    private fun isOfType(defaultFromClass: Class<*>, fromClass: Class<*>, toClass: Class<*>): Boolean {
        var trueFromClass: Class<*> = fromClass
        if (trueFromClass == Nothing::class.java) {
            check(defaultFromClass != Nothing::class.java) { "No mapping target or default mapping target specified" }
            trueFromClass = defaultFromClass
        }
        return trueFromClass.isAssignableFrom(toClass)
    }

    private fun getTransformer(
        annotation: MappedField,
        fromPair: ObjectFieldTrio,
        toPair: ObjectFieldTrio
    ): TransformerRegistration<*, *> {
        val transformationPair = fromPair to toPair
        log.trace("Attempting to find transformer for transformation pair [ $transformationPair ]")
        var transformerRegistration: TransformerRegistration<*, *> = TransformerRegistration.EMPTY
        log.trace("Checking transformerRef field")
        if (annotation.transformerRef.isNotBlank()) {
            log.trace("transformerRef is not empty with value [ " + annotation.transformerRef + " ]")
            transformerRegistration = getTransformerByName(annotation.transformerRef)
            if (transformerRegistration != TransformerRegistration.EMPTY) {
                log.trace("Found transformer by ref [ ${annotation.transformerRef} ] of type [ " + transformerRegistration.transformer.javaClass.name + " ]")
            } else {
                error("Could not find transformer by ref [ ${annotation.transformerRef} ] on $fromPair")
            }
        }
        if (transformerRegistration == TransformerRegistration.EMPTY) {
            log.trace("Checking transformer field")
            if (annotation.transformer == EmptyTransformer::class) {
                log.trace("Transformer is Empty Transformer, attempting to find a default transformer")
                val key = fromPair.type to toPair.type
                val defaultTransformerRegistration = defaultTransformers[key]
                if (defaultTransformerRegistration != null) {
                    log.trace("Found a default transformer of type [ " + defaultTransformerRegistration.transformer.javaClass.name + " ]")
                    return defaultTransformerRegistration
                }
                return TransformerRegistration.EMPTY
            } else {
                transformerRegistration = getTransformerByType(annotation.transformer.java)
                if (transformerRegistration != TransformerRegistration.EMPTY) {
                    log.trace("Found transformer by type [ ${annotation.transformer.java} ]")
                } else {
                    error("Could not find transformer by type [ ${annotation.transformer.java} ] on $fromPair")
                }
            }
        }
        return transformerRegistration
    }

    private fun <From : Any, To : Any> registerTransformer(registration: TransformerRegistration<From, To>) {
        val name = registration.name ?: registration.transformer::class.simpleName!!
        val newRegistration = registration.copy(name = name)
        val existingTransformer = getTransformerByName(name)
        if (existingTransformer != TransformerRegistration.EMPTY) {
            error("Transformer with name $name already exists with type ${existingTransformer.transformer::class}")
        }
        if (newRegistration.default) {
            val existingDefaultTransformer = defaultTransformers[newRegistration.transformer.id]
            if (existingDefaultTransformer != null) {
                error("Default transformer with pair ${newRegistration.transformer.id} already exists")
            }
            defaultTransformers[newRegistration.transformer.id] = newRegistration
        }

        transformers.add(newRegistration)
        transformersByNameCache.remove(name)
        transformersByTypeCache.remove(newRegistration.transformer::class.java)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ShapeShift::class.java)
        const val NODE_DELIMITER = "."
        val NODE_DELIMITER_REGEX = Regex("\\.")

        enum class SourceType {
            FROM,
            TO
        }
    }
}