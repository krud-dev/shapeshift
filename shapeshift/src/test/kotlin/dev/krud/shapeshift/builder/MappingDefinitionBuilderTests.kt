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
import dev.krud.shapeshift.ShapeShiftBuilder
import dev.krud.shapeshift.condition.MappingCondition
import dev.krud.shapeshift.condition.MappingConditionContext
import dev.krud.shapeshift.transformer.base.MappingTransformer
import dev.krud.shapeshift.transformer.base.MappingTransformerContext
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class MappingDefinitionBuilderTests {
    @Test
    internal fun `test simple happy flow`() {
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withMapping(
                MappingDefinitionBuilder(From::class.java, To::class.java)
                    .mapField("name", "name")
                    .mapField("age", "age")
                    .mapField("address.city", "city")
                    .build()
            )
            .build()
        val original = From()
        val result = shapeShift.map<From, To>(original)
        expectThat(result.name)
            .isEqualTo(original.name)
        expectThat(result.age)
            .isEqualTo(original.age)
        expectThat(result.city)
            .isEqualTo(original.address.city)
    }

    @Test
    internal fun `test mapping with condition`() {
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withMapping(
                MappingDefinitionBuilder(From::class.java, To::class.java)
                    .mapField("name", "name")
                    .mapField("age", "age").withCondition { ctx -> ctx.originalValue as Int > 31 }
                    .build()
            )
            .build()
        val original = From()
        val result = shapeShift.map<From, To>(original)
        expectThat(result.name)
            .isEqualTo(original.name)
        expectThat(result.age)
            .isNull()
    }

    @Test
    internal fun `test mapping with condition reference`() {
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withMapping(
                MappingDefinitionBuilder(From::class.java, To::class.java)
                    .mapField("name", "name")
                    .mapField("age", "age").withCondition(Above31Condition::class.java)
                    .build()
            )
            .build()
        val original = From()
        val result = shapeShift.map<From, To>(original)
        expectThat(result.name)
            .isEqualTo(original.name)
        expectThat(result.age)
            .isNull()
    }

    @Test
    internal fun `test mapping with transformer`() {
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withMapping(
                MappingDefinitionBuilder(From::class.java, To::class.java)
                    .mapField("age", "age").withTransformer { ctx -> ctx.originalValue as Int + 1 }
                    .build()
            )
            .build()
        val original = From()
        val result = shapeShift.map<From, To>(original)
        expectThat(result.age)
            .isEqualTo(original.age + 1)
    }

    @Test
    internal fun `test mapping with transformer reference`() {
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withTransformer(AddOneTransformer())
            .withMapping(
                MappingDefinitionBuilder(From::class.java, To::class.java)
                    .mapField("age", "age").withTransformer(AddOneTransformer::class.java)
                    .build()
            )
            .build()
        val original = From()
        val result = shapeShift.map<From, To>(original)
        expectThat(result.age)
            .isEqualTo(original.age + 1)
    }

    @Test
    internal fun `test mapping with override mapping strategy`() {
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withDefaultMappingStrategy(MappingStrategy.MAP_NOT_NULL)
            .withMapping(
                MappingDefinitionBuilder(From::class.java, To::class.java)
                    .mapField("profession", "profession").withMappingStrategy(MappingStrategy.MAP_ALL)
                    .build()
            )
            .build()
        val original = From()
        val result = shapeShift.map<From, To>(original)
        expectThat(result.profession)
            .isNull()
    }
}

class From(
    val name: String = "John",
    val age: Int = 30,
    val address: Address = Address(),
    val profession: String? = null
) {
    class Address {
        val city = "London"
    }
}

class To(
    var name: String? = null,
    var age: Int? = null,
    var city: String? = null,
    val profession: String? = "placeholder"
)

class AddOneTransformer : MappingTransformer<Int, Int> {
    override fun transform(context: MappingTransformerContext<out Int>): Int {
        return (context.originalValue as Int) + 1
    }
}

class Above31Condition : MappingCondition<Int> {
    override fun isValid(context: MappingConditionContext<Int>): Boolean {
        return context.originalValue != null && context.originalValue as Int > 31
    }
}