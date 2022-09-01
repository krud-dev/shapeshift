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

import dev.krud.shapeshift.MappingTransformerRegistration.Companion.toRegistration
import dev.krud.shapeshift.decorator.MappingDecorator
import dev.krud.shapeshift.transformer.base.MappingTransformer
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
        internal fun `annotation automatic mapping with implicit target`() {
            val shapeShift = ShapeShiftBuilder()
                .build()
            val from = SameTypeAutomaticMappingFromImplicit()
            val result = shapeShift.map<SameTypeAutomaticMappingFromImplicit, GenericTo>(from)
            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `annotation automatic mapping with explicit target`() {
            val shapeShift = ShapeShiftBuilder()
                .build()
            val from = SameTypeAutomaticMappingFromExplicit()
            val result = shapeShift.map<SameTypeAutomaticMappingFromExplicit, GenericTo>(from)
            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `annotation automatic mapping with explicit wrong target`() {
            val shapeShift = ShapeShiftBuilder()
                .build()
            val from = SameTypeAutomaticMappingFromExplicitWrongTarget()
            val result = shapeShift.map<SameTypeAutomaticMappingFromExplicitWrongTarget, GenericTo>(from)
            expectThat(result.long)
                .isNull()
        }

        @Test
        internal fun `mapCollection with set of objects`() {
            val shapeShift = ShapeShiftBuilder()
                .withMapping<GenericFrom, GenericTo> {
                    GenericFrom::long mappedTo GenericTo::long
                }
                .build()
            val result: List<GenericTo> = shapeShift.mapCollection(
                setOf(
                    GenericFrom(1L),
                    GenericFrom(2L),
                    GenericFrom(3L)
                )
            )
            val expected = listOf(
                GenericTo(1L),
                GenericTo(2L),
                GenericTo(3L)
            )
            expectThat(result)
                .isEqualTo(expected)
        }

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
                    LongToStringTransformer().toRegistration()
                )
                .build()

            val result = shapeShift.map(TypeTransformerFrom(), StringTo::class.java)

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `simple mapping with default transformer`() {
            shapeShift = ShapeShiftBuilder()
                .withTransformer(
                    LongToStringTransformer().toRegistration(true)
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

//    @Test
    internal fun `annotation automatic mapping with type mismatch should throw exception`() {
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .build()
        val from = NameOnlyTypeAutomaticMappingFromExplicitWrongTarget()
        expectThrows<IllegalStateException> {
            shapeShift.map<NameOnlyTypeAutomaticMappingFromExplicitWrongTarget, GenericTo>(from)
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
                FromWithInvalidFromPath(),
                GenericTo::class.java
            )
        }
    }

    @Test
    internal fun `supplying invalid to path should throw exception`() {
        expectThrows<NoSuchFieldException> {
            shapeShift.map(
                FromWithInvalidToPath(),
                GenericTo::class.java
            )
        }
    }

    @Test
    internal fun `registering default transformer twice with same pair should throw exception`() {
        val firstRegistration = ExampleFieldTransformer().toRegistration(true)

        val secondRegistration = ExampleFieldTransformer().toRegistration(true)

        val builder = ShapeShiftBuilder()
            .withTransformer(firstRegistration)
            .withTransformer(secondRegistration)

        expectThrows<IllegalStateException> {
            builder.build()
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

    @Test
    internal fun `test direct withDecorator overload happy flow registers the correct decorator`() {
        val decorator: MappingDecorator<GenericFrom, GenericTo> = MappingDecorator { }
        val shapeShift = ShapeShiftBuilder()
            .withDecorator(GenericFrom::class.java, GenericTo::class.java, decorator)
            .build()
        expectThat(shapeShift.decoratorRegistrations.first().decorator)
            .isEqualTo(decorator)
    }

    @Test
    internal fun `test direct withTransformers overload happy flow registers the correct decorator`() {
        val transformer: MappingTransformer<String, Int> = MappingTransformer { 1 }
        val shapeShift = ShapeShiftBuilder()
            .excludeDefaultTransformers()
            .withTransformer(String::class.java, Int::class.java, transformer)
            .build()
        expectThat(shapeShift.transformerRegistrations.first().transformer)
            .isEqualTo(transformer)
    }
}