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

import dev.krud.shapeshift.condition.Condition
import dev.krud.shapeshift.dto.MappingStructure
import dev.krud.shapeshift.dto.ObjectFieldTrio
import dev.krud.shapeshift.dto.ResolvedMappedField
import dev.krud.shapeshift.dto.TransformerCoordinates
import dev.krud.shapeshift.resolver.MappingResolver
import dev.krud.shapeshift.transformer.base.ClassPair
import dev.krud.shapeshift.transformer.base.FieldTransformer
import dev.krud.shapeshift.transformer.base.FieldTransformer.Companion.id
import dev.krud.shapeshift.util.getValue
import dev.krud.shapeshift.util.setValue
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

class ShapeShift constructor(
    transformersRegistrations: Set<TransformerRegistration<out Any, out Any>> = emptySet(),
    val mappingResolvers: Set<MappingResolver> = setOf()
) {
    internal val transformers: MutableList<TransformerRegistration<out Any, out Any>> = mutableListOf()
    internal val transformersByNameCache: MutableMap<String, TransformerRegistration<out Any, out Any>> = mutableMapOf()
    internal val transformersByTypeCache: MutableMap<Class<out FieldTransformer<*, *>>, TransformerRegistration<*, *>> =
        mutableMapOf()
    internal val defaultTransformers: MutableMap<ClassPair, TransformerRegistration<out Any, out Any>> = mutableMapOf()
    private val mappingStructures: MutableMap<ClassPair, MappingStructure> = mutableMapOf()
    private val entityFieldsCache: MutableMap<Class<*>, Map<String, Field>> = mutableMapOf()
    private val conditionCache: MutableMap<Class<out Condition<*>>, Condition<*>> = mutableMapOf()

    init {
        for (registration in transformersRegistrations) {
            registerTransformer(registration)
        }
    }

    inline fun <reified To : Any> map(fromObject: Any): To {
        return map(fromObject, To::class.java)
    }

    fun <To : Any> map(fromObject: Any, toClazz: Class<To>): To {
        val toObject = toClazz.newInstance()
        return map(fromObject, toObject)
    }

    private fun <To : Any> map(fromObject: Any, toObject: To): To {
        val toClazz = toObject::class.java
        val mappingStructure = getMappingStructure(fromObject::class.java, toClazz)

        for (resolvedMappedField in mappingStructure.resolvedMappedFields) {
            processMappedField(resolvedMappedField, fromObject, toObject)
        }

        return toObject
    }

    private fun <To : Any, From : Any> processMappedField(
        resolvedMappedField: ResolvedMappedField,
        fromObject: From,
        toObject: To
    ) {
        val fromPair = getFieldInstanceByNodes(resolvedMappedField.mapFromCoordinates, fromObject, SourceType.FROM) ?: return
        val toPair = getFieldInstanceByNodes(resolvedMappedField.mapToCoordinates, toObject, SourceType.TO) ?: return
        val transformerRegistration = getTransformer(resolvedMappedField.transformerCoordinates, fromPair, toPair)
        mapField(fromPair, toPair, transformerRegistration, resolvedMappedField)
    }

    private fun mapField(fromPair: ObjectFieldTrio, toPair: ObjectFieldTrio, transformerRegistration: TransformerRegistration<*, *>, resolvedMappedField: ResolvedMappedField) {
        fromPair.field.isAccessible = true
        toPair.field.isAccessible = true
        var value = fromPair.field.getValue(fromPair.target)
        val condition = resolvedMappedField.condition
            ?: if (resolvedMappedField.conditionClazz != null) {
                getConditionInstance(resolvedMappedField.conditionClazz)
            } else {
                null
            }

        if (condition != null) {
            condition as Condition<Any>
            if (!condition.isValid(value)) {
                return
            }
        }

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
            MappingStructure(fromClass, toClass, mappingResolvers.flatMap { it.resolve(fromClass, toClass) })
        }
    }

    private fun getTransformer(
        coordinates: TransformerCoordinates,
        fromPair: ObjectFieldTrio,
        toPair: ObjectFieldTrio
    ): TransformerRegistration<*, *> {
        val transformationPair = fromPair to toPair
        log.trace("Attempting to find transformer for transformation pair [ $transformationPair ]")
        var transformerRegistration: TransformerRegistration<*, *> = TransformerRegistration.EMPTY
        log.trace("Checking transformerRef field")
        if (!coordinates.name.isNullOrBlank()) {
            log.trace("transformerRef is not empty with value [ " + coordinates.name + " ]")
            transformerRegistration = getTransformerByName(coordinates.name)
            if (transformerRegistration != TransformerRegistration.EMPTY) {
                log.trace("Found transformer by ref [ ${coordinates.name} ] of type [ " + transformerRegistration.transformer.javaClass.name + " ]")
            } else {
                error("Could not find transformer by ref [ ${coordinates.name} ] on $fromPair")
            }
        }
        if (transformerRegistration == TransformerRegistration.EMPTY) {
            log.trace("Checking transformer field")
            if (coordinates.type == null) {
                log.trace("Transformer is null, attempting to find a default transformer")
                val key = fromPair.type to toPair.type
                val defaultTransformerRegistration = defaultTransformers[key]
                if (defaultTransformerRegistration != null) {
                    log.trace("Found a default transformer of type [ " + defaultTransformerRegistration.transformer.javaClass.name + " ]")
                    return defaultTransformerRegistration
                }
                return TransformerRegistration.EMPTY
            } else {
                transformerRegistration = getTransformerByType(coordinates.type)
                if (transformerRegistration != TransformerRegistration.EMPTY) {
                    log.trace("Found transformer by type [ ${coordinates.type} ]")
                } else {
                    error("Could not find transformer by type [ ${coordinates.type} ] on $fromPair")
                }
            }
        }
        return transformerRegistration
    }

    private fun getConditionInstance(conditionClazz: Class<out Condition<*>>): Condition<*> {
        return conditionCache.computeIfAbsent(conditionClazz) {
            conditionClazz.newInstance()
        }
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

        enum class SourceType {
            FROM,
            TO
        }
    }
}