/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.builder

import dev.krud.shapeshift.MappingStrategy
import dev.krud.shapeshift.condition.MappingCondition
import dev.krud.shapeshift.dto.ResolvedMappedField
import dev.krud.shapeshift.dto.TransformerCoordinates
import dev.krud.shapeshift.enums.AutoMappingStrategy
import dev.krud.shapeshift.resolver.MappingDefinition
import dev.krud.shapeshift.transformer.base.MappingTransformer
import dev.krud.shapeshift.util.getAutoMappings
import dev.krud.shapeshift.util.getDeclaredFieldRecursive
import java.lang.reflect.Field

/**
 * A builder of mapping definitions for non-Kotlin use
 */
class MappingDefinitionBuilder(val fromClazz: Class<out Any>, val toClazz: Class<out Any>) {
    private val resolvedMappedFields = mutableListOf<ResolvedMappedField>()
    private var autoMappingStrategy: AutoMappingStrategy = AutoMappingStrategy.NONE

    /**
     * Automatically map all fields with the given strategy
     */
    fun autoMap(strategy: AutoMappingStrategy): MappingDefinitionBuilder {
        this.autoMappingStrategy = strategy
        return this
    }

    /**
     * Automatically map all fields with the given strategy with strategy [AutoMappingStrategy.BY_NAME_AND_TYPE]
     */
    fun autoMap(): MappingDefinitionBuilder {
        return autoMap(AutoMappingStrategy.BY_NAME_AND_TYPE)
    }

    /**
     * Define a new mapped field, along with an optional condition, transformer and override mapping strategy
     * @see MapFieldBuilder
     * @param from The field to map from
     * @param to The field to map to
     */
    fun mapField(from: String, to: String): MapFieldBuilder {
        val mapFieldBuilder = MapFieldBuilder(from, to)
        return mapFieldBuilder
    }

    /**
     * Return a mapping definition for the fields defined in this builder
     */
    fun build(): MappingDefinition {
        val autoMappedFields = getAutoMappings(fromClazz, toClazz, autoMappingStrategy)
            .filter { autoResolvedMappedField ->
                resolvedMappedFields.none {
                    it.mapFromCoordinates.first() == autoResolvedMappedField.mapFromCoordinates.first() || it.mapToCoordinates.first() == autoResolvedMappedField.mapToCoordinates.first()
                }
            }
        return MappingDefinition(
            fromClazz,
            toClazz,
            resolvedMappedFields + autoMappedFields
        )
    }

    inner class MapFieldBuilder(val from: String, val to: String) {
        private var condition: MappingCondition<out Any>? = null
        private var conditionClazz: Class<out MappingCondition<out Any>>? = null
        private var transformer: MappingTransformer<out Any, out Any>? = null
        private var transformerCoordinates: TransformerCoordinates = TransformerCoordinates.NONE
        private var mappingStrategy: MappingStrategy? = null

        /**
         * @see MappingDefinitionBuilder.autoMap
         */
        fun autoMap(strategy: AutoMappingStrategy): MappingDefinitionBuilder {
            buildAndAddSelf()
            return this@MappingDefinitionBuilder.autoMap(strategy)
        }

        /**
         * @see MappingDefinitionBuilder.autoMap
         */
        fun autoMap(): MappingDefinitionBuilder {
            buildAndAddSelf()
            return this@MappingDefinitionBuilder.autoMap()
        }

        /**
         * @see MappingDefinitionBuilder.mapField
         */
        fun mapField(from: String, to: String): MapFieldBuilder {
            buildAndAddSelf()
            return this@MappingDefinitionBuilder.mapField(from, to)
        }

        /**
         * Specify a condition to use for this mapped field
         * Java Example: `withCondition(ctx -> (Integer) ctx.originalValue > 18)`
         */
        fun withCondition(condition: MappingCondition<out Any>): MapFieldBuilder {
            this.condition = condition
            return this
        }

        /**
         * Specify a condition reference to use for this mapped field
         * Java Example: `withCondition(Above18Condition.class)`
         */
        fun withCondition(conditionClazz: Class<out MappingCondition<out Any>>): MapFieldBuilder {
            this.conditionClazz = conditionClazz
            return this
        }

        /**
         * Specify a transformer to use for this mapped field
         * Java Example: `withTransformer(ctx -> (Integer) ctx.originalValue.toString())`
         */
        fun withTransformer(transformer: MappingTransformer<out Any, out Any>): MapFieldBuilder {
            this.transformer = transformer
            return this
        }

        /**
         * Specify a transformer reference to use for this mapped field
         * Java Example: `withTransformer(IntegerToStringTransoformer.class)`
         */
        fun withTransformer(transformerClazz: Class<out MappingTransformer<out Any, out Any>>): MapFieldBuilder {
            this.transformerCoordinates = TransformerCoordinates.ofType(transformerClazz)
            return this
        }

        /**
         * Specify an override mapping strategy for this mapped field
         */
        fun withMappingStrategy(mappingStrategy: MappingStrategy): MapFieldBuilder {
            this.mappingStrategy = mappingStrategy
            return this
        }

        /**
         * @see MappingDefinitionBuilder.build
         */
        fun build(): MappingDefinition {
            buildAndAddSelf()
            return this@MappingDefinitionBuilder.build()
        }

        private fun buildAndAddSelf() {
            val resolvedMappedField = ResolvedMappedField(
                resolveNodes(from.split("."), fromClazz),
                resolveNodes(to.split("."), toClazz),
                transformerCoordinates,
                transformer,
                conditionClazz,
                condition,
                mappingStrategy
            )
            this@MappingDefinitionBuilder.resolvedMappedFields.add(resolvedMappedField)
        }
    }

    private fun resolveNodes(nodes: List<String>, clazz: Class<*>): List<Field> {
        if (nodes.isEmpty()) {
            return emptyList()
        }
        val realField = clazz.getDeclaredFieldRecursive(nodes.first())
        return listOf(realField) + resolveNodes(nodes.drop(1), realField.type)
    }
}