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

import dev.krud.shapeshift.MappingStrategy
import dev.krud.shapeshift.ShapeShiftBuilder
import dev.krud.shapeshift.condition.MappingCondition
import dev.krud.shapeshift.condition.MappingConditionContext
import dev.krud.shapeshift.transformer.base.MappingTransformer
import dev.krud.shapeshift.transformer.base.MappingTransformerContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.reflect.jvm.javaField

internal class KotlinDslMappingDefinitionBuilderTests {
    @Nested
    inner class Scenarios {
        @Test
        internal fun `simple mapping without transformer`() {
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    From::string mappedTo To::string
                    From::child..From.Child::string mappedTo To::child..To.Child::string
                }
                .build()
            val source = From()

            val result = shapeShift.map<From, To>(source)
            expectThat(result.string)
                .isEqualTo(source.string)
            expectThat(result.child.string)
                .isEqualTo(source.child.string)
        }

        @Test
        internal fun `simple mapping with ad hoc transformer`() {
            val expected = "transformed"
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    From::string mappedTo To::string withTransformer {
                        expected
                    }
                }
                .build()

            val result = shapeShift.map<From, To>(From())
            expectThat(result.string)
                .isEqualTo(expected)
        }

        @Test
        internal fun `simple mapping with transformer class reference`() {
            val expected = "SampleTransformer"
            val shapeShift = ShapeShiftBuilder()
                .withTransformer(SampleTransformer())
                .withMapping<From, To> {
                    From::string mappedTo To::string withTransformer SampleTransformer::class
                }
                .build()

            val result = shapeShift.map<From, To>(From())
            expectThat(result.string)
                .isEqualTo(expected)
        }

        @Test
        internal fun `simple mapping with falsey ad hoc condition`() {
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    From::string mappedTo To::string withCondition {
                        false
                    }
                }
                .build()

            val result = shapeShift.map<From, To>(From())
            expectThat(result.string)
                .isEqualTo("Unmodified")
        }

        @Test
        internal fun `simple mapping ٍwith truthy ad hoc condition`() {
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    From::string mappedTo To::string withCondition {
                        true
                    }
                }
                .build()
            val source = From()
            val result = shapeShift.map<From, To>(source)
            expectThat(result.string)
                .isEqualTo(source.string)
        }

        @Test
        internal fun `simple mapping ٍwith truthy condition class reference`() {
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    From::string mappedTo To::string withCondition AlwaysTrueCondition::class
                }
                .build()
            val source = From()
            val result = shapeShift.map<From, To>(source)
            expectThat(result.string)
                .isEqualTo(source.string)
        }

        @Test
        internal fun `simple mapping with falsey condition class reference`() {
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    From::string mappedTo To::string withCondition AlwaysFalseCondition::class
                }
                .build()
            val result = shapeShift.map<From, To>(From())
            expectThat(result.string)
                .isEqualTo("Unmodified")
        }

        @Test
        internal fun `ad hoc decorator without additional mappings`() {
            val expected = "decorated"
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    decorate {
                        it.to.string = expected
                    }
                }
                .build()
            val result = shapeShift.map<From, To>(From())
            expectThat(result.string)
                .isEqualTo(expected)
        }

        @Test
        internal fun `ad hoc decorator with additional mappings`() {
            val expected = "decorated"
            val shapeShift = ShapeShiftBuilder()
                .withMapping<From, To> {
                    decorate {
                        it.to.string = expected
                    }
                    From::string mappedTo To::string
                }
                .build()
            val result = shapeShift.map<From, To>(From())
            expectThat(result.string)
                .isEqualTo("decorated")
        }

        @Test
        internal fun `simple mapping with MAP_ALL overrideStrategy`() {
            val shapeShift = ShapeShiftBuilder()
                .withDefaultMappingStrategy(MappingStrategy.MAP_NOT_NULL)
                .withMapping<From, To> {
                    From::nullableString mappedTo To::nullableString overrideStrategy MappingStrategy.MAP_ALL
                }
                .build()
            val result = shapeShift.map<From, To>(From(nullableString = null))
            expectThat(result.nullableString)
                .isEqualTo(null)
        }

        @Test
        internal fun `simple mapping with MAP_NOT_NULL overrideStrategy`() {
            val shapeShift = ShapeShiftBuilder()
                .withDefaultMappingStrategy(MappingStrategy.MAP_ALL)
                .withMapping<From, To> {
                    From::nullableString mappedTo To::nullableString overrideStrategy MappingStrategy.MAP_NOT_NULL
                }
                .build()
            val source = From()
            val result = shapeShift.map<From, To>(source)
            expectThat(result.nullableString)
                .isEqualTo(source.nullableString)
        }
    }

    @Test
    fun `rangeTo on source should give a reference to the child field`() {
        val mapping = mapper<From, To> {
            From::child..From.Child::string mappedTo To::string
        }

        val resolvedMappedField = mapping.mappingDefinition.resolvedMappedFields.first()
        expectThat(resolvedMappedField.mapFromCoordinates)
            .isEqualTo(listOf(From::child.javaField!!, From.Child::string.javaField!!))
    }

    @Test
    fun `rangeTo on target should give a reference to the child field`() {
        val mapping = mapper<From, To> {
            From::string mappedTo To::child..To.Child::string
        }

        val resolvedMappedField = mapping.mappingDefinition.resolvedMappedFields.first()
        expectThat(resolvedMappedField.mapToCoordinates)
            .isEqualTo(listOf(To::child.javaField!!, To.Child::string.javaField!!))
    }

    @Test
    fun `complex multi-level rangeTo on source should give a reference to the child field`() {
        val mapping = mapper<From, To> {
            From::child..From.Child::grandChild..From.GrandChild::string mappedTo To::string
        }

        val resolvedMappedField = mapping.mappingDefinition.resolvedMappedFields.first()
        expectThat(resolvedMappedField.mapFromCoordinates)
            .isEqualTo(listOf(From::child.javaField!!, From.Child::grandChild.javaField!!, From.GrandChild::string.javaField!!))
    }

    @Test
    internal fun `test nullable mapping is correct single level`() {
        val mapping = mapper<From, To> {
            From::nullableChild..From.Child::string mappedTo To::string
        }

        val resolvedMappedField = mapping.mappingDefinition.resolvedMappedFields.first()
        expectThat(resolvedMappedField.mapFromCoordinates)
            .isEqualTo(listOf(From::nullableChild.javaField!!, From.Child::string.javaField!!))
    }

    @Test
    internal fun `test nullable mapping is correct single level with nullable child`() {
        val mapping = mapper<From, To> {
            From::nullableChild..From.Child::nullableString mappedTo To::string
        }

        val resolvedMappedField = mapping.mappingDefinition.resolvedMappedFields.first()
        expectThat(resolvedMappedField.mapFromCoordinates)
            .isEqualTo(listOf(From::nullableChild.javaField!!, From.Child::nullableString.javaField!!))
    }

    @Test
    internal fun `test nullable mapping is correct multi level`() {
        val mapping = mapper<From, To> {
            From::nullableChild..From.Child::nullableGrandChild..From.GrandChild::string mappedTo To::string
        }

        val resolvedMappedField = mapping.mappingDefinition.resolvedMappedFields.first()
        expectThat(resolvedMappedField.mapFromCoordinates)
            .isEqualTo(listOf(From::nullableChild.javaField!!, From.Child::nullableGrandChild.javaField!!, From.GrandChild::string.javaField!!))
    }

    @Test
    internal fun `test nullable mapping is correct multi level with nullable grandchild`() {
        val mapping = mapper<From, To> {
            From::nullableChild..From.Child::nullableGrandChild..From.GrandChild::nullableString mappedTo To::string
        }

        val resolvedMappedField = mapping.mappingDefinition.resolvedMappedFields.first()
        expectThat(resolvedMappedField.mapFromCoordinates)
            .isEqualTo(listOf(From::nullableChild.javaField!!, From.Child::nullableGrandChild.javaField!!, From.GrandChild::nullableString.javaField!!))
    }
}

internal class AlwaysTrueCondition : MappingCondition<String> {
    override fun isValid(context: MappingConditionContext<String>): Boolean {
        return true
    }
}

internal class AlwaysFalseCondition : MappingCondition<String> {
    override fun isValid(context: MappingConditionContext<String>): Boolean {
        return false
    }
}

internal class SampleTransformer : MappingTransformer<String, String> {
    override fun transform(context: MappingTransformerContext<out String>): String {
        return "SampleTransformer"
    }
}

internal class From(
    var string: String = "Test 1",
    var nullableString: String? = "Nullable Test 1",
    var child: Child = Child(),
    var nullableChild: Child? = Child()
) {
    class Child(
        var string: String = "Test 2",
        var nullableString: String? = "Nullable Test 2",
        var grandChild: GrandChild = GrandChild(),
        var nullableGrandChild: GrandChild? = GrandChild()
    )

    class GrandChild(
        var string: String = "Test 3",
        var nullableString: String? = "Nullable Test 3"
    )
}

internal class To(
    var string: String = "Unmodified",
    var nullableString: String? = "Nullable Test 2",
    var child: Child = Child()
) {
    class Child(
        var string: String = "Unmodified",
        var grandChild: GrandChild = GrandChild()
    )

    class GrandChild(
        var string: String = "Unmodified"
    )
}