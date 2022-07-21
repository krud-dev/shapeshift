/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.dsl

import dev.krud.shapeshift.MappingDecoratorRegistration
import dev.krud.shapeshift.MappingStrategy
import dev.krud.shapeshift.condition.MappingCondition
import dev.krud.shapeshift.decorator.MappingDecorator
import dev.krud.shapeshift.dto.ResolvedMappedField
import dev.krud.shapeshift.dto.TransformerCoordinates
import dev.krud.shapeshift.resolver.MappingDefinition
import dev.krud.shapeshift.transformer.base.MappingTransformer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

/**
 * The builder powering the Kotlin Mapping DSL
 */
@KotlinMappingDsl
class KotlinDslMappingDefinitionBuilder<RootFrom : Any, RootTo : Any>(
    private val fromClazz: Class<RootFrom>,
    private val toClazz: Class<RootTo>
) {
    private val fieldMappings = mutableListOf<FieldMapping<*, *>>()
    private val decoratorRegistrations: MutableSet<MappingDecoratorRegistration<RootFrom, RootTo>> = mutableSetOf()

    /**
     * Helper operator function to access sub-fields of a field.
     * For example: `House::person..Person::name`
     */
    operator fun <Root : Any, Child : Any, ChildValue : Any?> KProperty1<Root, Child?>.rangeTo(other: KProperty1<Child, ChildValue?>): FieldCoordinates<Root, Child, ChildValue?> {
        return FieldCoordinates(mutableListOf(this, other) as MutableList<KProperty1<Child, ChildValue?>>)
    }

    operator fun <Root : Any, Parent : Any, Child : Any, ChildValue : Any?> FieldCoordinates<Root, Parent, Child?>.rangeTo(other: KProperty1<Child, ChildValue?>): FieldCoordinates<Root, Child, ChildValue?> {
        this.fields.add(other as KProperty1<Parent, Child>)
        return this as FieldCoordinates<Root, Child, ChildValue?>
    }

    /**
     * Creates a mapping between two fields
     */

    infix fun <From : Any, FromValue : Any, To : Any, ToValue : Any> KProperty1<From, FromValue?>.mappedTo(to: FieldCoordinates<RootTo, To, ToValue?>): FieldMapping<FromValue?, ToValue?> {
        // KProperty1 -> FieldCoordinates
        return toFieldCoordinates<RootFrom, From, FromValue>().mappedTo(to)
    }

    /**
     * Creates a mapping between two fields
     */
    infix fun <From : Any, FromValue : Any, To : Any, ToValue : Any> FieldCoordinates<RootFrom, From, FromValue?>.mappedTo(to: KProperty1<To, ToValue?>): FieldMapping<FromValue?, ToValue?> {
        // FieldCoordinates -> KProperty1
        return this.mappedTo(to.toFieldCoordinates())
    }

    /**
     * Creates a mapping between two fields
     */
    infix fun <From : Any, FromValue : Any, To : Any, ToValue : Any> KProperty1<From, FromValue?>.mappedTo(to: KProperty1<To, ToValue?>): FieldMapping<FromValue?, ToValue?> {
        // KProperty1 -> KProperty1
        return this.toFieldCoordinates<RootFrom, From, FromValue>().mappedTo(to.toFieldCoordinates())
    }

    /**
     * Creates a mapping between two fields
     */
    infix fun <From : Any, FromValue : Any, To : Any, ToValue : Any> FieldCoordinates<RootFrom, From, FromValue?>.mappedTo(to: FieldCoordinates<RootTo, To, ToValue?>): FieldMapping<FromValue?, ToValue?> {
        // FieldCoordinates -> FieldCoordinates
        val fieldMapping = FieldMapping(
            this,
            to,
            null,
            null,
            null,
            null,
            null
        )
        fieldMappings.add(fieldMapping)
        return fieldMapping
    }

    /**
     * Defines a decorator for the given pair of classes.
     */
    fun decorate(decorator: MappingDecorator<RootFrom, RootTo>) {
        this.decoratorRegistrations += MappingDecoratorRegistration(fromClazz, toClazz, decorator)
    }

    /**
     * Specify a transformer class to use for the given maping
     */
    infix fun <From : Any?, To : Any?> FieldMapping<From, out To>.withTransformer(transformer: KClass<out MappingTransformer<out From, out To>>): FieldMapping<From, out To> {
        this.transformerClazz = transformer as KClass<Nothing>
        return this
    }

    /**
     * Define a transformer to use for the given mapping
     */
    infix fun <From : Any?, To : Any?> FieldMapping<From, To>.withTransformer(transformer: MappingTransformer<out From, out To>): FieldMapping<From, out To> {
        this.transformer = transformer
        return this
    }

    /**
     * Specify a condition class to use for the given mapping
     */
    infix fun <From : Any?, To : Any?> FieldMapping<From, out To>.withCondition(condition: KClass<out MappingCondition<out From>>): FieldMapping<From, out To> {
        this.conditionClazz = condition
        return this
    }

    /**
     * Define a condition to use for the given mapping
     */
    infix fun <From : Any?, To : Any?> FieldMapping<From, out To>.withCondition(condition: MappingCondition<From>): FieldMapping<From, out To> {
        this.condition = condition
        return this
    }

    /**
     * Specify a mapping strategy to use for the given mapping.
     */
    infix fun <From : Any?, To : Any?> FieldMapping<From, out To>.overrideStrategy(mappingStrategy: MappingStrategy): FieldMapping<From, out To> {
        this.mappingStrategy = mappingStrategy
        return this
    }

    fun build(): Result {
        return Result(
            MappingDefinition(
                fromClazz,
                toClazz,
                fieldMappings.map { fieldMapping ->
                    ResolvedMappedField(
                        fieldMapping.fromField.fields.map { it.javaField!! },
                        fieldMapping.toField.fields.map { it.javaField!! },
                        if (fieldMapping.transformerClazz == null) {
                            TransformerCoordinates.NONE
                        } else {
                            TransformerCoordinates.ofType(fieldMapping.transformerClazz!!.java)
                        },
                        fieldMapping.transformer,
                        fieldMapping.conditionClazz?.java,
                        fieldMapping.condition,
                        fieldMapping.mappingStrategy
                    )
                }
            ),
            decoratorRegistrations
        )
    }

    private fun <Root : Any, Field : Any, Value : Any> KProperty1<Field, Value?>.toFieldCoordinates(): FieldCoordinates<Root, Field, Value?> {
        val result: FieldCoordinates<Root, Field, Value?> = FieldCoordinates(mutableListOf(this))
        return result
    }

    companion object {
        class FieldCoordinates<Root, LastField : Any, LastValue : Any?>(
            val fields: MutableList<KProperty1<LastField, LastValue>> = mutableListOf()
        )

        class FieldMapping<FromValue : Any?, ToValue : Any?>(
            var fromField: FieldCoordinates<*, *, FromValue>,
            var toField: FieldCoordinates<*, *, ToValue>,
            var transformerClazz: KClass<out MappingTransformer<FromValue, ToValue>>?,
            var transformer: MappingTransformer<out FromValue, out ToValue>?,
            var conditionClazz: KClass<out MappingCondition<out FromValue>>?,
            var condition: MappingCondition<out FromValue>?,
            var mappingStrategy: MappingStrategy?
        )

        data class Result(
            val mappingDefinition: MappingDefinition,
            val decoratorRegistrations: Set<MappingDecoratorRegistration<*, *>>
        )
    }
}

inline fun <reified From : Any, reified To : Any> mapper(block: KotlinDslMappingDefinitionBuilder<From, To>.() -> Unit): KotlinDslMappingDefinitionBuilder.Companion.Result {
    val builder = KotlinDslMappingDefinitionBuilder(From::class.java, To::class.java)
    builder.block()
    return builder.build()
}