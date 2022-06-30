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

import dev.krud.shapeshift.decorator.MappingDecorator
import dev.krud.shapeshift.dsl.KotlinDslMappingDefinitionBuilder
import dev.krud.shapeshift.resolver.MappingDefinition
import dev.krud.shapeshift.resolver.MappingDefinitionResolver
import dev.krud.shapeshift.resolver.annotation.AnnotationMappingDefinitionResolver
import dev.krud.shapeshift.transformer.base.FieldTransformer

class ShapeShiftBuilder {
    private val transformers: MutableSet<TransformerRegistration<*, *>> = mutableSetOf()
    private val decorators: MutableSet<MappingDecorator<*, *>> = mutableSetOf()
    private val resolvers: MutableSet<MappingDefinitionResolver> = mutableSetOf()
    private var defaultMappingStrategy: MappingStrategy = MappingStrategy.MAP_NOT_NULL
    private val mappingDefinitions: MutableList<MappingDefinition> = mutableListOf()

    init {
        withResolver(AnnotationMappingDefinitionResolver())
    }

    fun withDefaultMappingStrategy(defaultMappingStrategy: MappingStrategy): ShapeShiftBuilder {
        this.defaultMappingStrategy = defaultMappingStrategy
        return this
    }

    fun withDecorator(decorator: MappingDecorator<out Any, out Any>) : ShapeShiftBuilder {
        decorators += decorator
        return this
    }

    fun withTransformer(fieldTransformer: FieldTransformer<out Any, out Any>, default: Boolean = false, name: String? = null): ShapeShiftBuilder {
        transformers += TransformerRegistration(fieldTransformer, default, name)
        return this
    }

    fun withTransformer(transformerRegistration: TransformerRegistration<*, *>): ShapeShiftBuilder {
        transformers += transformerRegistration
        return this
    }

    fun withMapping(vararg mappingDefinitions: MappingDefinition): ShapeShiftBuilder {
        this.mappingDefinitions.addAll(mappingDefinitions)
        return this
    }

    inline fun <reified From : Any, reified To : Any> withMapping(block: KotlinDslMappingDefinitionBuilder<From, To>.() -> Unit): ShapeShiftBuilder {
        val builder = KotlinDslMappingDefinitionBuilder(From::class.java, To::class.java)
        builder.block()
        val (mappingDefinitions, decorators) = builder.build()
        withMapping(mappingDefinitions)
        decorators.forEach { withDecorator(it) }
        return this
    }

    fun withResolver(mappingDefinitionResolver: MappingDefinitionResolver): ShapeShiftBuilder {
        resolvers.add(mappingDefinitionResolver)
        return this
    }

    fun build(): ShapeShift {
        return ShapeShift(transformers, resolvers, defaultMappingStrategy, decorators)
    }
}