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

import dev.krud.shapeshift.annotation.DefaultMappingTarget
import dev.krud.shapeshift.annotation.MappedField
import dev.krud.shapeshift.transformer.base.FieldTransformer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message
import java.lang.reflect.Field

internal class FieldMapperKtTests {
    internal lateinit var mapper: FieldMapper

    @BeforeEach
    internal fun setUp() {
        mapper = FieldMapper()
    }

    @Nested
    inner class Scenarios {
        @Test
        internal fun `multiple mapped fields on field`() {
            val result = mapper.map(FromWithMultipleMappedFields(), MultipleFieldTo::class.java)
            expectThat(result.long)
                .isEqualTo(1L)
            expectThat(result.secondLong)
                .isEqualTo(1L)
        }

        @Test
        internal fun `simple mapping without transformer`() {
            val result = mapper.map(TransformerlessFrom(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `simple mapping with transformer by type`() {
            mapper.registerTransformer(
                TransformerRegistration(
                    LongToStringTransformer()
                )
            )
            val result = mapper.map(TypeTransformerFrom(), StringTo::class.java)

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `simple mapping with transformer by name`() {
            mapper.registerTransformer(
                TransformerRegistration(
                    LongToStringTransformer(),
                    name = "myTransformer"
                )
            )
            val result = mapper.map(NameTransformerFrom(), StringTo::class.java)

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `simple mapping with default transformer`() {
            mapper.registerTransformer(
                TransformerRegistration(
                    LongToStringTransformer(),
                    default = true
                )
            )
            val result = mapper.map(DefaultTransformerFrom(), StringTo::class.java)

            expectThat(result.long)
                .isEqualTo("1")
        }

        @Test
        internal fun `simple mapping on type level`() {
            val result = mapper.map(TransformerlessTypeLevelFrom(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `simple mapping with default target mapping`() {
            val result = mapper.map(FromWithDefaultMappingTarget(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `complex path mapping on mapFrom`() {
            val result = mapper.map(FromWithComplexPath(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `complex path mapping on mapTo`() {
            val result = mapper.map(FromToComplexPath(), ToWithComplexPath::class.java)

            expectThat(result.child.grandchild?.greatGrandchild?.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapFrom with self field qualifier`() {
            val result = mapper.map(FromWithMapFromSelfQualifier(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapFrom with self field qualifier on type level`() {
            val result = mapper.map(FromWithMapFromSelfQualifierOnType(), GenericTo::class.java)

            expectThat(result.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapTo with self field qualifier`() {
            val result = mapper.map(FromWithMapToSelfQualifier(), ToWithShallowPath::class.java)

            expectThat(result.child?.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `mapTo with self field qualifier on type level`() {
            val result = mapper.map(FromWithMapToSelfQualifierOnType(), ToWithShallowPath::class.java)

            expectThat(result.child?.long)
                .isEqualTo(1L)
        }

        @Test
        internal fun `nested class mapping`() {
            val result = mapper.map(
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
            val result = mapper.map(FromWithNullField(), ToWithPopulatedField::class.java)
            expectThat(result.long)
                .isEqualTo(1L)
        }
    }

    @Test
    internal fun `mismatch between from and to types should throw exception`() {
        expectThrows<IllegalStateException> {
            mapper.map(
                DefaultTransformerFrom(),
                StringTo::class.java
            )
        }
    }

    @Test
    internal fun `supplying invalid from path should throw exception`() {
        expectThrows<IllegalStateException> {
            mapper.map(
                FromWithInvalidFromPath(), GenericTo::class.java
            )
        }
    }

    @Test
    internal fun `supplying invalid to path should throw exception`() {
        expectThrows<IllegalStateException> {
            mapper.map(
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
        mapper.registerTransformer(firstRegistration)
        expectThrows<IllegalStateException> {
            mapper.registerTransformer(secondRegistration)
        }
    }

    @Test
    internal fun `registering transformer with null name should use simple class name when registering`() {
        val registration = TransformerRegistration(
            ExampleFieldTransformer(),
            false,
            null
        )
        mapper.registerTransformer(registration)
        expectThat(mapper.transformers.first().name)
            .isEqualTo(
                "ExampleFieldTransformer"
            )

    }


    @Test
    internal fun `registering same transformer twice should throw exception`() {
        val registration = TransformerRegistration(
            ExampleFieldTransformer(),
            false,
            "first"
        )

        mapper.registerTransformer(registration)
        expectThrows<IllegalStateException> {
            mapper.registerTransformer(registration)
        }
    }


    @Test
    internal fun `type level mapped field throws exception if mapFrom is empty`() {
        expectThrows<IllegalStateException> {
            mapper.map(FromWithInvalidTypeAnnotation(), GenericTo::class.java)
        }.and {
            this.message.isEqualTo("mapFrom can not be empty when used at a type level")
        }
    }

    @Test
    internal fun `field level mapped field without default mapping target and no target should throw exception`() {
        expectThrows<IllegalStateException> {
            mapper.map(FromWithoutDefinedTarget(), GenericTo::class.java)
        }
    }

    @Test
    internal fun `type level mapped field without default mapping target and no target should throw exception`() {
        expectThrows<IllegalStateException> {
            mapper.map(TypeFromWithoutDefinedTarget(), GenericTo::class.java)
        }
    }

    @DefaultMappingTarget(GenericTo::class)
    internal class FromWithDefaultMappingTarget {
        @MappedField
        val long: Long = 1L
    }

    internal class FromWithNullField {
        @MappedField(target = ToWithPopulatedField::class)
        val long: Long? = null
    }

    internal class ToWithPopulatedField {
        val long: Long? = 1L
    }

    internal abstract class BaseFromWithMappedField {
        @MappedField(target = BaseTo::class)
        val baseLong: Long = 1L
    }

    internal class FromWithBase : BaseFromWithMappedField() {
        @MappedField(target = ToWithBase::class)
        val long: Long = 1L
    }


    internal class FromWithComplexPath {
        @MappedField(target = GenericTo::class, mapFrom = "grandchild.greatGrandchild.long")
        val child: Child = Child()

        class Child {
            val grandchild: Grandchild = Grandchild()

            class Grandchild {
                val greatGrandchild: GreatGrandchild = GreatGrandchild()
                class GreatGrandchild {
                    val long: Long = 1L
                }
            }
        }
    }

    internal class FromToComplexPath {
        @MappedField(target = ToWithComplexPath::class, mapTo = "child.grandchild.greatGrandchild.long")
        val long: Long = 1L
    }

    @MappedField(target = GenericTo::class)
    internal class FromWithInvalidTypeAnnotation

    internal class FromWithoutDefinedTarget {
        @MappedField
        val long: Long = 1L
    }

    @MappedField(mapFrom = "long")
    internal class TypeFromWithoutDefinedTarget {
        val long: Long = 1L
    }

    internal class FromWithInvalidFromPath {
        @MappedField(target = GenericTo::class, mapFrom = "i.am.invalid")
        val long: Long = 1L
    }

    internal class FromWithInvalidToPath {
        @MappedField(target = GenericTo::class, mapTo = "i.am.invalid")
        val long: Long = 1L
    }

    internal class FromWithMultipleMappedFields {
        @MappedField(target = MultipleFieldTo::class)
        @MappedField(target = MultipleFieldTo::class, mapTo = "secondLong")
        val long: Long = 1L
    }

    internal class FromWithMapFromSelfQualifier {
        @MappedField(target = GenericTo::class, mapFrom = "child.long")
        val child: Child = Child()
        class Child {
            val long: Long = 1L
        }
    }

    @MappedField(target = GenericTo::class, mapFrom = "fromWithMapFromSelfQualifierOnType.child.long")
    internal class FromWithMapFromSelfQualifierOnType {
        val child: Child = Child()
        class Child {
            val long: Long = 1L
        }
    }

    internal class FromWithMapToSelfQualifier {
        @MappedField(target = ToWithShallowPath::class, mapTo = "toWithShallowPath.child.long")
        val long: Long = 1L
    }

    @MappedField(target = ToWithShallowPath::class, mapFrom="long", mapTo = "toWithShallowPath.child.long")
    internal class FromWithMapToSelfQualifierOnType {
        val long: Long = 1L
    }

    internal class TransformerlessFrom {
        @MappedField(target = GenericTo::class)
        val long: Long = 1L

    }

    @MappedField(target = GenericTo::class, mapFrom = "long")
    internal class TransformerlessTypeLevelFrom {
        val long: Long = 1L

    }

    internal class NoTargetFrom {
        @MappedField
        val long = 1L
    }

    internal class DefaultTransformerFrom {
        @MappedField(target = StringTo::class)
        val long: Long = 1L

    }

    internal class NameTransformerFrom {
        @MappedField(target = StringTo::class, transformerRef = "myTransformer")
        val long: Long = 1L
    }

    internal class TypeTransformerFrom {
        @MappedField(target = StringTo::class, transformer = LongToStringTransformer::class)
        val long: Long = 1L
    }

    internal class GenericTo {
        var long: Long? = null
    }

    internal class StringTo {
        var long: String? = null
    }

    internal class MultipleFieldTo {
        val long: Long? = null
        val secondLong: Long? = null
    }

    internal class ToWithShallowPath {
        val child: Child? = null

        class Child {
            val long: Long? = null
        }
    }

    internal class ToWithComplexPath {
        val child: Child = Child()

        class Child {
            val grandchild: Grandchild? = null

            class Grandchild {
                val greatGrandchild: GreatGrandchild? = null
                class GreatGrandchild {
                    val long: Long? = null
                }
            }
        }
    }

    internal abstract class BaseTo {
        val baseLong: Long? = null
    }

    internal class ToWithBase : BaseTo() {
        val long: Long? = null
    }

    internal class LongToStringTransformer : FieldTransformer<Long, String> {
        override val fromType: Class<Long> = Long::class.java

        override val toType: Class<String> = String::class.java

        override fun transform(
            fromField: Field,
            toField: Field,
            originalValue: Long?,
            fromObject: Any,
            toObject: Any
        ): String? {
            return originalValue?.toString()
        }
    }
}