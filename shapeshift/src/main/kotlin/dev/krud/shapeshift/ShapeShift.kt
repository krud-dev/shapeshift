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

import dev.krud.shapeshift.MappingDecoratorRegistration.Companion.id
import dev.krud.shapeshift.MappingTransformerRegistration.Companion.id
import dev.krud.shapeshift.condition.MappingCondition
import dev.krud.shapeshift.condition.MappingConditionContext
import dev.krud.shapeshift.decorator.MappingDecorator
import dev.krud.shapeshift.decorator.MappingDecoratorContext
import dev.krud.shapeshift.dto.MappingStructure
import dev.krud.shapeshift.dto.ObjectFieldTrio
import dev.krud.shapeshift.dto.ResolvedMappedField
import dev.krud.shapeshift.dto.TransformerCoordinates
import dev.krud.shapeshift.resolver.MappingDefinitionResolver
import dev.krud.shapeshift.transformer.base.MappingTransformer
import dev.krud.shapeshift.transformer.base.MappingTransformerContext
import dev.krud.shapeshift.util.ClassPair
import dev.krud.shapeshift.util.concurrentMapOf
import dev.krud.shapeshift.util.getValue
import dev.krud.shapeshift.util.setValue
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

class ShapeShift internal constructor(
    transformersRegistrations: Set<MappingTransformerRegistration<out Any, out Any>>,
    val mappingDefinitionResolvers: Set<MappingDefinitionResolver>,
    val defaultMappingStrategy: MappingStrategy,
    val decoratorRegistrations: Set<MappingDecoratorRegistration<out Any, out Any>>
) {
    val transformerRegistrations: MutableList<MappingTransformerRegistration<out Any, out Any>> = mutableListOf()
    internal val transformersByTypeCache: MutableMap<Class<out MappingTransformer<out Any?, out Any?>>, MappingTransformerRegistration<out Any?, out Any?>> =
        concurrentMapOf()
    internal val defaultTransformers: MutableMap<ClassPair<out Any, out Any>, MappingTransformerRegistration<out Any, out Any>> = mutableMapOf()
    private val mappingStructures: MutableMap<ClassPair<out Any, out Any>, MappingStructure> = concurrentMapOf()
    private val conditionCache: MutableMap<Class<out MappingCondition<*>>, MappingCondition<*>> = concurrentMapOf()
    private val decoratorCache: MutableMap<ClassPair<out Any, out Any>, List<MappingDecorator<*, *>>> = concurrentMapOf()

    init {
        if (defaultMappingStrategy == MappingStrategy.NONE) {
            error("Default mapping strategy cannot be NONE")
        }
        for (registration in transformersRegistrations) {
            registerTransformer(registration)
        }
    }

    inline fun <From : Any, reified To : Any> map(fromObject: From): To {
        return map(fromObject, To::class.java)
    }

    /**
     * Map between the [fromObject] and a new instance of [toClazz]
     * [toClazz] MUST have a no-arg constructor when using this override
     */
    fun <From : Any, To : Any> map(fromObject: From, toClazz: Class<To>): To {
        val toObject = toClazz.newInstance()
        return map(fromObject, toObject)
    }

    /**
     * Map between the [fromObject] and [toObject] objects
     */
    fun <From : Any, To : Any> map(fromObject: From, toObject: To): To {
        val toClazz = toObject::class.java
        val classPair = ClassPair(fromObject::class.java, toClazz)
        val mappingStructure = getMappingStructure(fromObject::class.java, toClazz)

        for (resolvedMappedField in mappingStructure.resolvedMappedFields) {
            mapField(fromObject, toObject, resolvedMappedField)
        }

        val decorators = getDecorators<From, To>(classPair)
        if (decorators.isNotEmpty()) {
            val context = MappingDecoratorContext(fromObject, toObject)
            for (decorator in decorators) {
                decorator.decorate(context)
            }
        }

        return toObject
    }

    private fun <From : Any, To : Any> mapField(fromObject: From, toObject: To, resolvedMappedField: ResolvedMappedField) {
        val fromPair = getFieldInstanceByNodes(resolvedMappedField.mapFromCoordinates, fromObject, SourceType.FROM) ?: return
        val toPair = getFieldInstanceByNodes(resolvedMappedField.mapToCoordinates, toObject, SourceType.TO) ?: return
        val transformerRegistration = getTransformer(resolvedMappedField.transformerCoordinates, fromPair, toPair)
        fromPair.field.isAccessible = true
        toPair.field.isAccessible = true

        val mappingStrategy: MappingStrategy

        if (resolvedMappedField.overrideMappingStrategy != null && resolvedMappedField.overrideMappingStrategy != MappingStrategy.NONE) {
            mappingStrategy = resolvedMappedField.overrideMappingStrategy
        } else {
            mappingStrategy = defaultMappingStrategy
        }
        val fromValue = fromPair.field.getValue(fromPair.target)

        val shouldMap = when (mappingStrategy) {
            MappingStrategy.NONE -> error("Mapping strategy is set to NONE")
            MappingStrategy.MAP_ALL -> true
            MappingStrategy.MAP_NOT_NULL -> fromValue != null
        }

        if (shouldMap) {
            try {
                val condition = resolvedMappedField.condition
                    ?: if (resolvedMappedField.conditionClazz != null) {
                        getConditionInstance(resolvedMappedField.conditionClazz)
                    } else {
                        null
                    }

                if (condition != null) {
                    condition as MappingCondition<Any>
                    val context = MappingConditionContext(fromValue)
                    if (!condition.isValid(context)) {
                        return
                    }
                }

                val valueToSet = if (resolvedMappedField.transformer != null) {
                    val transformer = resolvedMappedField.transformer as MappingTransformer<Any, Any>
                    val context = MappingTransformerContext(fromObject, toObject, fromPair.field, toPair.field, fromValue)
                    transformer.transform(context)
                } else if (transformerRegistration != MappingTransformerRegistration.EMPTY) {
                    val transformer = transformerRegistration.transformer as MappingTransformer<Any, Any>
                    val context = MappingTransformerContext(fromObject, toObject, fromPair.field, toPair.field, fromValue)
                    transformer.transform(context)
                } else {
                    fromValue
                }

                if (valueToSet == null) {
                    toPair.field.setValue(toPair.target, null)
                } else {
                    if (!toPair.type.isAssignableFrom(valueToSet::class.java)) {
                        error("Type mismatch: Expected ${toPair.type} but got ${valueToSet::class.java}")
                    }
                    toPair.field.setValue(toPair.target, valueToSet)
                }
            } catch (e: Exception) {
                val newException =
                    IllegalStateException("Could not map value ${fromPair.field.name} of class ${fromPair.target.javaClass.simpleName} to ${toPair.field.name} of class ${toPair.target.javaClass.simpleName}: ${e.message}")
                newException.initCause(e)
                throw newException
            }
        }
    }

    private fun getFieldInstanceByNodes(nodes: List<Field>, target: Any?, type: SourceType): ObjectFieldTrio? {
        // This if only applies to recursive runs of this function
        // When target is null and type is from, don't attempt to instantiate the object
        if (target == null) {
            if (type == SourceType.FROM) {
                return null
            } else {
                // Impossible to reach
                error("$nodes leads to a null target")
            }
        }
        val field = nodes.first()

        val fieldType = field.type.kotlin.javaObjectType

        if (nodes.size == 1) {
            return ObjectFieldTrio(target, field, fieldType)
        }
        field.isAccessible = true
        var subTarget = field.get(target)

        if (subTarget == null && type == SourceType.TO) {
            subTarget = fieldType.newInstance()
            field.set(target, subTarget)
        }

        return getFieldInstanceByNodes(nodes.drop(1), subTarget, type)
    }

    private fun getTransformerByType(type: Class<out MappingTransformer<out Any?, out Any?>>): MappingTransformerRegistration<out Any?, out Any?> {
        return transformersByTypeCache.computeIfAbsent(type) { _ ->
            transformerRegistrations.find { it.transformer::class.java == type } ?: MappingTransformerRegistration.EMPTY
        }
    }

    private fun getMappingStructure(fromClass: Class<*>, toClass: Class<*>): MappingStructure {
        val key = ClassPair(fromClass, toClass)
        return mappingStructures.computeIfAbsent(key) {
            val resolutions = mappingDefinitionResolvers
                .mapNotNull { it.resolve(fromClass, toClass) }

            MappingStructure(fromClass, toClass, resolutions.flatMap { it.resolvedMappedFields })
        }
    }

    private fun <From : Any, To : Any> getDecorators(classPair: ClassPair<From, To>): List<MappingDecorator<From, To>> {
        return decoratorCache.computeIfAbsent(classPair) {
            decoratorRegistrations
                .filter { decoratorRegistration ->
                    val id = decoratorRegistration.id
                    id == classPair
                }
                .map { decoratorRegistrations ->
                    decoratorRegistrations.decorator
                }
        } as List<MappingDecorator<From, To>>
    }

    private fun getTransformer(
        coordinates: TransformerCoordinates,
        fromPair: ObjectFieldTrio,
        toPair: ObjectFieldTrio
    ): MappingTransformerRegistration<*, *> {
        val transformationPair = fromPair to toPair
        log.trace("Attempting to find transformer for transformation pair [ $transformationPair ]")
        var transformerRegistration: MappingTransformerRegistration<*, *> = MappingTransformerRegistration.EMPTY
        if (transformerRegistration == MappingTransformerRegistration.EMPTY) {
            log.trace("Checking transformer field")
            if (coordinates.type == null) {
                log.trace("Transformer is null, attempting to find a default transformer")
                val key = ClassPair(fromPair.type, toPair.type)
                val defaultTransformerRegistration = defaultTransformers[key]
                if (defaultTransformerRegistration != null) {
                    log.trace("Found a default transformer of type [ " + defaultTransformerRegistration.transformer.javaClass.name + " ]")
                    return defaultTransformerRegistration
                }
                return MappingTransformerRegistration.EMPTY
            } else {
                transformerRegistration = getTransformerByType(coordinates.type)
                if (transformerRegistration != MappingTransformerRegistration.EMPTY) {
                    log.trace("Found transformer by type [ ${coordinates.type} ]")
                } else {
                    error("Could not find transformer by type [ ${coordinates.type} ] on $fromPair")
                }
            }
        }
        return transformerRegistration
    }

    private fun getConditionInstance(conditionClazz: Class<out MappingCondition<*>>): MappingCondition<*> {
        return conditionCache.computeIfAbsent(conditionClazz) {
            conditionClazz.newInstance()
        }
    }

    private fun <From : Any, To : Any> registerTransformer(registration: MappingTransformerRegistration<From, To>) {
        if (registration.default) {
            val existingDefaultTransformer = defaultTransformers[registration.id]
            if (existingDefaultTransformer != null) {
                error("Default transformer with pair ${registration.id} already exists")
            }
            defaultTransformers[registration.id] = registration
        }

        transformerRegistrations.add(registration)
        transformersByTypeCache.remove(registration.transformer::class.java)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ShapeShift::class.java)

        enum class SourceType {
            FROM,
            TO
        }
    }
}