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

import dev.krud.shapeshift.TransformerRegistration.Companion.toRegistration
import dev.krud.shapeshift.decorator.MappingDecorator
import dev.krud.shapeshift.dsl.KotlinDslMappingDefinitionBuilder
import dev.krud.shapeshift.resolver.MappingDefinition
import dev.krud.shapeshift.resolver.MappingDefinitionResolver
import dev.krud.shapeshift.resolver.StaticMappingDefinitionResolver
import dev.krud.shapeshift.resolver.annotation.AnnotationMappingDefinitionResolver
import dev.krud.shapeshift.transformer.AnyToStringMappingTransformer
import dev.krud.shapeshift.transformer.DateToLongMappingTransformer
import dev.krud.shapeshift.transformer.LongToDateMappingTransformer
import dev.krud.shapeshift.transformer.NumberToCharMappingTransformer
import dev.krud.shapeshift.transformer.NumberToDoubleMappingTransformer
import dev.krud.shapeshift.transformer.NumberToFloatMappingTransformer
import dev.krud.shapeshift.transformer.NumberToIntMappingTransformer
import dev.krud.shapeshift.transformer.NumberToLongMappingTransformer
import dev.krud.shapeshift.transformer.NumberToShortMappingTransformer
import dev.krud.shapeshift.transformer.StringToBooleanMappingTransformer
import dev.krud.shapeshift.transformer.StringToCharMappingTransformer
import dev.krud.shapeshift.transformer.StringToDoubleMappingTransformer
import dev.krud.shapeshift.transformer.StringToFloatMappingTransformer
import dev.krud.shapeshift.transformer.StringToIntMappingTransformer
import dev.krud.shapeshift.transformer.StringToLongMappingTransformer
import dev.krud.shapeshift.transformer.StringToShortMappingTransformer
import dev.krud.shapeshift.transformer.base.MappingTransformer

/**
 * A builder used to create a new ShapeShift instance.
 * By default, the builder will add all default transformers, and the [AnnotationMappingDefinitionResolver] as a resolver
 * for mapping definitions.
 */
class ShapeShiftBuilder {
    private val transformers: MutableSet<TransformerRegistration<out Any, out Any>> = mutableSetOf()
    private val decorators: MutableSet<MappingDecorator<*, *>> = mutableSetOf()
    private val resolvers: MutableSet<MappingDefinitionResolver> = mutableSetOf()
    private var defaultMappingStrategy: MappingStrategy = MappingStrategy.MAP_NOT_NULL
    private val mappingDefinitions: MutableList<MappingDefinition> = mutableListOf()

    init {
        // Add default annotation resolver
        withResolver(AnnotationMappingDefinitionResolver())

        // Add default transformers
        DEFAULT_TRANSFORMERS.forEach {
            withTransformer(it)
        }
    }

    /**
     * Set the default mapping strategy for the ShapeShift instance
     */
    fun withDefaultMappingStrategy(defaultMappingStrategy: MappingStrategy): ShapeShiftBuilder {
        this.defaultMappingStrategy = defaultMappingStrategy
        return this
    }

    /**
     * Add a decorator to the ShapeShift instance
     */
    fun withDecorator(decorator: MappingDecorator<out Any, out Any>): ShapeShiftBuilder {
        decorators += decorator
        return this
    }

    /**
     * Add a transformer to the ShapeShift instance
     */
    inline fun <reified From : Any, reified To : Any> withTransformer(mappingTransformer: MappingTransformer<From, To>, default: Boolean = false, name: String? = null): ShapeShiftBuilder {
        return withTransformer(mappingTransformer.toRegistration(default, name))
    }

    /**
     * Add a resolver to the ShapeShift instance
     */
    fun withTransformer(transformerRegistration: TransformerRegistration<out Any, out Any>): ShapeShiftBuilder {
        transformers += transformerRegistration
        return this
    }

    /**
     * Add new mapping definitions to the ShapeShift instance
     */
    fun withMapping(vararg mappingDefinitions: MappingDefinition): ShapeShiftBuilder {
        this.mappingDefinitions.addAll(mappingDefinitions)
        return this
    }

    /**
     * Add a new mapping definition to the ShapeShift instance using the Kotlin DSL
     */
    inline fun <reified From : Any, reified To : Any> withMapping(block: KotlinDslMappingDefinitionBuilder<From, To>.() -> Unit): ShapeShiftBuilder {
        val builder = KotlinDslMappingDefinitionBuilder(From::class.java, To::class.java)
        builder.block()
        val (mappingDefinitions, decorators) = builder.build()
        withMapping(mappingDefinitions)
        decorators.forEach { withDecorator(it) }
        return this
    }

    /**
     * Add a new mapping definition resolver to the ShapeShift instance
     */
    fun withResolver(mappingDefinitionResolver: MappingDefinitionResolver): ShapeShiftBuilder {
        resolvers.add(mappingDefinitionResolver)
        return this
    }

    /**
     * Remove all default transformers from the ShapeShift instance
     */
    fun excludeDefaultTransformers(): ShapeShiftBuilder {
        transformers.removeAll { it in DEFAULT_TRANSFORMERS }
        return this
    }

    /**
     * Return the ShapeShift instance
     */
    fun build(): ShapeShift {
        if (defaultMappingStrategy == MappingStrategy.NONE) {
            throw IllegalArgumentException("Default mapping strategy cannot be NONE")
        }

        if (mappingDefinitions.isNotEmpty()) {
            resolvers += StaticMappingDefinitionResolver(mappingDefinitions)
        }

        return ShapeShift(transformers, resolvers, defaultMappingStrategy, decorators)
    }

    companion object {
        private val DEFAULT_TRANSFORMERS = setOf<TransformerRegistration<out Any, out Any>>(
            AnyToStringMappingTransformer().toRegistration(true),
            StringToBooleanMappingTransformer().toRegistration(true),
            StringToCharMappingTransformer().toRegistration(true),
            StringToDoubleMappingTransformer().toRegistration(true),
            StringToFloatMappingTransformer().toRegistration(true),
            StringToIntMappingTransformer().toRegistration(true),
            StringToLongMappingTransformer().toRegistration(true),
            StringToShortMappingTransformer().toRegistration(true),
            LongToDateMappingTransformer().toRegistration(true),
            DateToLongMappingTransformer().toRegistration(true),
            NumberToCharMappingTransformer().toRegistration(true),
            NumberToDoubleMappingTransformer().toRegistration(true),
            NumberToFloatMappingTransformer().toRegistration(true),
            NumberToLongMappingTransformer().toRegistration(true),
            NumberToShortMappingTransformer().toRegistration(true),
            NumberToIntMappingTransformer().toRegistration(true)
        )
    }
}