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

import dev.krud.shapeshift.condition.MappingCondition
import dev.krud.shapeshift.condition.MappingConditionContext
import dev.krud.shapeshift.resolver.annotation.DefaultMappingTarget
import dev.krud.shapeshift.resolver.annotation.MappedField
import dev.krud.shapeshift.transformer.base.MappingTransformer
import dev.krud.shapeshift.transformer.base.MappingTransformerContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

internal class ShapeShiftTests {
    internal lateinit var shapeShift: ShapeShift

    @BeforeEach
    internal fun setUp() {
        shapeShift = ShapeShiftBuilder().build()
    }

    @Nested
    inner class Scenarios {
        @Test
        internal fun `multiple mapped fields on field`() {
            val result = shapeShift.map(FromWithMultipleMappedFields(), MultipleFieldTo::class.java)
            expectThat(result.long)
                .isEqualTo(1L)
            expectThat(result.secondLong)
                .isEqualTo(1L)
        }

        @Test
        internal fun `simple mapping without transformer`() {
            val result = shapeShift.map(TransformerlessFrom(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `simple mapping with transformer by type`() {
            shapeShift = ShapeShiftBuilder()
                .withTransformer(
                    TransformerRegistration(
                        LongToStringTransformer()
                    )
                )
                .build()

            val result = shapeShift.map(TypeTransformerFrom(), StringTo::class.java)

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `simple mapping with transformer by name`() {
            shapeShift = ShapeShiftBuilder()
                .withTransformer(
                    TransformerRegistration(
                        LongToStringTransformer(),
                        name = "myTransformer"
                    )
                )
                .build()
            val result = shapeShift.map(NameTransformerFrom(), StringTo::class.java)

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `simple mapping with default transformer`() {
            shapeShift = ShapeShiftBuilder()
                .withTransformer(
                    TransformerRegistration(
                        LongToStringTransformer(),
                        default = true
                    )
                )
                .build()
            val result = shapeShift.map(DefaultTransformerFrom(), StringTo::class.java)

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `simple mapping on type level`() {
            val result = shapeShift.map(TransformerlessTypeLevelFrom(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `simple mapping with default target mapping`() {
            val result = shapeShift.map(FromWithDefaultMappingTarget(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `complex path mapping on mapFrom`() {
            val result = shapeShift.map(FromWithComplexPath(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `complex path mapping on mapTo`() {
            val result = shapeShift.map(FromToComplexPath(), ToWithComplexPath::class.java)

            expectThat(result.child.grandchild?.greatGrandchild?.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `nested class mapping`() {
            val result = shapeShift.map(
                FromWithBase(),
                ToWithBase::class.java
            )

            expectThat(result.long)
                .isEqualTo(1L)

            expectThat(result.baseLong)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapping null object`() {
            val result = shapeShift.map(FromWithNullField(), ToWithPopulatedField::class.java)
            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapping nested null object`() {
            val result = shapeShift.map(FromWithNullShallowPath(), ToWithPopulatedField::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapped field with truthy condition`() {
            val result = shapeShift.map(FromWithTruthyCondition(), GenericTo::class.java)
            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapped field with falsy condition`() {
            val result = shapeShift.map(ToWithFalsyCondition(), GenericTo::class.java)

            expectThat(result.long)
                .isNull()
        }

        @Test
        internal fun `dsl with ad hoc transformer`() {
            val shapeShift = ShapeShiftBuilder()
                .excludeDefaultTransformers()
                .withMapping<GenericFrom, StringTo> {
                    GenericFrom::long mappedTo StringTo::long withTransformer {
                        it.originalValue.toString()
                    }
                }
                .build()
            val result = shapeShift.map<GenericFrom, StringTo>(GenericFrom())

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `ad hoc decorator`() {
            val shapeShift = ShapeShiftBuilder()
                .excludeDefaultTransformers()
                .withMapping<GenericFrom, StringTo> {
                    decorate {
                        val (_, to) = it
                        to.long = "123"
                    }
                }
                .build()
            val result = shapeShift.map<GenericFrom, StringTo>(GenericFrom())

            expectThat(result.long)
                .isEqualTo("123")
        }
    }

    @Test
    internal fun `mismatch between from and to types should throw exception`() {
        expectThrows<IllegalStateException> {
            shapeShift.map(
                DefaultTransformerFrom(),
                StringTo::class.java
            )
        }
    }

    @Test
    internal fun `supplying invalid from path should throw exception`() {
        expectThrows<NoSuchFieldException> {
            shapeShift.map(
                FromWithInvalidFromPath(), GenericTo::class.java
            )
        }
    }

    @Test
    internal fun `supplying invalid to path should throw exception`() {
        expectThrows<NoSuchFieldException> {
            shapeShift.map(
                FromWithInvalidToPath(), GenericTo::class.java
            )
        }
    }

    @Test
    internal fun `registering default transformer twice with same pair should throw exception`() {
        val firstRegistration = TransformerRegistration(
            ExampleFieldTransformer(),
            true,
            "first"
        )

        val secondRegistration = TransformerRegistration(
            ExampleFieldTransformer(),
            true,
            "second"
        )

        val builder = ShapeShiftBuilder()
            .withTransformer(firstRegistration)
            .withTransformer(secondRegistration)

        expectThrows<IllegalStateException> {
            builder.build()
        }
    }

    @Test
    internal fun `registering transformer with null name should use simple class name when registering`() {
        val registration = TransformerRegistration(
            ExampleFieldTransformer(),
            false,
            null
        )
        shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withTransformer(registration)
            .build()
        expectThat(shapeShift.transformers.first().name)
            .isEqualTo(
                "ExampleFieldTransformer"
            )
    }

    @Test
    internal fun `registering same transformer twice should result in 1 transformer`() {
        val registration = TransformerRegistration(
            ExampleFieldTransformer(),
            false,
            "first"
        )

        val builder = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withTransformer(registration)
            .withTransformer(registration)

        val shapeShift = builder.build()
        expectThat(shapeShift.transformers.size)
            .isEqualTo(1)
    }

    @Test
    internal fun `registering transformer with an existing name twice should throw exception`() {
        val registration = TransformerRegistration(
            ExampleFieldTransformer(),
            false,
            "first"
        )

        val secondRegistration = TransformerRegistration(
            ExampleFieldTransformer(),
            false,
            "first"
        )

        val builder = ShapeShiftBuilder()
            .withTransformer(registration)
            .withTransformer(secondRegistration)

        expectThrows<IllegalStateException> {
            builder.build()
        }
    }

    @Test
    internal fun `using unregistered transformer by name should throw exception`() {
        expectThrows<IllegalStateException> {
            shapeShift.map(NameTransformerFrom(), StringTo::class.java)
        }
    }

    @Test
    internal fun `using unregistered transformer by type should throw exception`() {
        expectThrows<IllegalStateException> {
            shapeShift.map(TypeTransformerFrom(), StringTo::class.java)
        }
    }

    @Test
    internal fun `type level mapped field throws exception if mapFrom is empty`() {
        expectThrows<IllegalStateException> {
            shapeShift.map(FromWithInvalidTypeAnnotation(), GenericTo::class.java)
        }
    }

    @Test
    internal fun `field level mapped field without default mapping target and no target should throw exception`() {
        expectThrows<IllegalStateException> {
            shapeShift.map(FromWithoutDefinedTarget(), GenericTo::class.java)
        }
    }

    @Test
    internal fun `type level mapped field without default mapping target and no target should throw exception`() {
        expectThrows<IllegalStateException> {
            shapeShift.map(TypeFromWithoutDefinedTarget(), GenericTo::class.java)
        }
    }
}