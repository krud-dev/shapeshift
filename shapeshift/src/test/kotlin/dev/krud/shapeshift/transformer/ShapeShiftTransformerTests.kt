/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.transformer

import dev.krud.shapeshift.ShapeShiftBuilder
import dev.krud.shapeshift.transformer.base.MappingTransformerContext
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.util.*
import kotlin.reflect.jvm.javaField

class ShapeShiftTransformerTests {
    @Test
    internal fun `AnyToStringMappingTransformer should return null if value is null`() {
        val transformer = AnyToStringMappingTransformer()
        val context = mockMappingTransformerContext<Long>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `AnyToStringMappingTransformer should stringify value`() {
        val transformer = AnyToStringMappingTransformer()
        val context = mockMappingTransformerContext(123)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo("123")
    }

    @Test
    internal fun `StringToBooleanMappingTransformer should return null if value is null`() {
        val transformer = StringToBooleanMappingTransformer()
        val context = mockMappingTransformerContext<String>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `StringToBooleanMappingTransformer should return correct boolean value`() {
        val transformer = StringToBooleanMappingTransformer()
        val context = mockMappingTransformerContext("true")
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(true)
    }

    @Test
    internal fun `StringToBooleanMappingTransformer should throw exception if value is not boolean`() {
        val transformer = StringToBooleanMappingTransformer()
        val context = mockMappingTransformerContext("123")
        expectThrows<IllegalArgumentException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToCharMappingTransformer should return null if value is null`() {
        val transformer = StringToCharMappingTransformer()
        val context = mockMappingTransformerContext<String>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `StringToCharMappingTransformer should return correct char value`() {
        val transformer = StringToCharMappingTransformer()
        val context = mockMappingTransformerContext("a")
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo('a')
    }

    @Test
    internal fun `StringToCharMappingTransformer should throw exception if value is not char`() {
        val transformer = StringToCharMappingTransformer()
        val context = mockMappingTransformerContext("123")
        expectThrows<IllegalArgumentException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToDoubleMappingTransformer should return null if value is null`() {
        val transformer = StringToDoubleMappingTransformer()
        val context = mockMappingTransformerContext<String>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `StringToDoubleMappingTransformer should return correct double value`() {
        val transformer = StringToDoubleMappingTransformer()
        val context = mockMappingTransformerContext("123.45")
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123.45)
    }

    @Test
    internal fun `StringToDoubleMappingTransformer should throw exception if value is not double`() {
        val transformer = StringToDoubleMappingTransformer()
        val context = mockMappingTransformerContext("test")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToFloatMappingTransformer should return null if value is null`() {
        val transformer = StringToFloatMappingTransformer()
        val context = mockMappingTransformerContext<String>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `StringToFloatMappingTransformer should return correct float value`() {
        val transformer = StringToFloatMappingTransformer()
        val context = mockMappingTransformerContext("123.45")
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123.45f)
    }

    @Test
    internal fun `StringToFloatMappingTransformer should throw exception if value is not float`() {
        val transformer = StringToFloatMappingTransformer()
        val context = mockMappingTransformerContext("test")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToIntMappingTransformer should return null if value is null`() {
        val transformer = StringToIntMappingTransformer()
        val context = mockMappingTransformerContext<String>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `StringToIntMappingTransformer should return correct int value`() {
        val transformer = StringToIntMappingTransformer()
        val context = mockMappingTransformerContext("123")
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123)
    }

    @Test
    internal fun `StringToIntMappingTransformer should throw exception if value exceeds int range`() {
        val transformer = StringToIntMappingTransformer()
        val context = mockMappingTransformerContext(Int.MAX_VALUE.toString() + "1")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToIntMappingTransformer should throw exception if value is not int`() {
        val transformer = StringToIntMappingTransformer()
        val context = mockMappingTransformerContext("test")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToLongMappingTransformer should return null if value is null`() {
        val transformer = StringToLongMappingTransformer()
        val context = mockMappingTransformerContext<String>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `StringToLongMappingTransformer should return correct long value`() {
        val transformer = StringToLongMappingTransformer()
        val context = mockMappingTransformerContext("123")
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123L)
    }

    @Test
    internal fun `StringToLongMappingTransformer should throw exception if value exceeds long range`() {
        val transformer = StringToLongMappingTransformer()
        val context = mockMappingTransformerContext(Long.MAX_VALUE.toString() + "1")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToLongMappingTransformer should throw exception if value is not long`() {
        val transformer = StringToLongMappingTransformer()
        val context = mockMappingTransformerContext("test")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToShortMappingTransformer should return null if value is null`() {
        val transformer = StringToShortMappingTransformer()
        val context = mockMappingTransformerContext<String>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `StringToShortMappingTransformer should return correct short value`() {
        val transformer = StringToShortMappingTransformer()
        val context = mockMappingTransformerContext("123")
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123)
    }

    @Test
    internal fun `StringToShortMappingTransformer should throw exception if value exceeds short range`() {
        val transformer = StringToShortMappingTransformer()
        val context = mockMappingTransformerContext(Short.MAX_VALUE.toString() + "1")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `StringToShortMappingTransformer should throw exception if value is not short`() {
        val transformer = StringToShortMappingTransformer()
        val context = mockMappingTransformerContext("test")
        expectThrows<NumberFormatException> {
            transformer.transform(context)
        }
    }

    @Test
    internal fun `NumberToCharMappingTransformer should return null if value is null`() {
        val transformer = NumberToCharMappingTransformer()
        val context = mockMappingTransformerContext<Number>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `NumberToCharMappingTransformer should return correct char value`() {
        val transformer = NumberToCharMappingTransformer()
        val context = mockMappingTransformerContext(123)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(Char(123))
    }

    @Test
    internal fun `NumberToDoubleMappingTransformer should return null if value is null`() {
        val transformer = NumberToDoubleMappingTransformer()
        val context = mockMappingTransformerContext<Number>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `NumberToDoubleMappingTransformer should return correct double value`() {
        val transformer = NumberToDoubleMappingTransformer()
        val context = mockMappingTransformerContext(123)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123.0)
    }

    @Test
    internal fun `NumberToFloatMappingTransformer should return null if value is null`() {
        val transformer = NumberToFloatMappingTransformer()
        val context = mockMappingTransformerContext<Number>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `NumberToFloatMappingTransformer should return correct float value`() {
        val transformer = NumberToFloatMappingTransformer()
        val context = mockMappingTransformerContext(123)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123.0f)
    }

    @Test
    internal fun `NumberToIntMappingTransformer should return null if value is null`() {
        val transformer = NumberToIntMappingTransformer()
        val context = mockMappingTransformerContext<Number>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `NumberToIntMappingTransformer should return correct int value`() {
        val transformer = NumberToIntMappingTransformer()
        val context = mockMappingTransformerContext(123.35)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123)
    }

    @Test
    internal fun `NumberToLongMappingTransformer should return null if value is null`() {
        val transformer = NumberToLongMappingTransformer()
        val context = mockMappingTransformerContext<Number>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `NumberToLongMappingTransformer should return correct long value`() {
        val transformer = NumberToLongMappingTransformer()
        val context = mockMappingTransformerContext(123.35)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123L)
    }

    @Test
    internal fun `NumberToShortMappingTransformer should return null if value is null`() {
        val transformer = NumberToShortMappingTransformer()
        val context = mockMappingTransformerContext<Number>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `NumberToShortMappingTransformer should return correct short value`() {
        val transformer = NumberToShortMappingTransformer()
        val context = mockMappingTransformerContext(123.35)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123)
    }

    @Test
    internal fun `DateToLongMappingTransformer should return null if value is null`() {
        val transformer = DateToLongMappingTransformer()
        val context = mockMappingTransformerContext<Date>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `DateToLongMappingTransformer should return correct long value`() {
        val transformer = DateToLongMappingTransformer()
        val context = mockMappingTransformerContext(Date(123))
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(123L)
    }

    @Test
    internal fun `LongToDateMappingTransformer should return null if value is null`() {
        val transformer = LongToDateMappingTransformer()
        val context = mockMappingTransformerContext<Long>(null)
        val result = transformer.transform(context)
        expectThat(result)
            .isNull()
    }

    @Test
    internal fun `LongToDateMappingTransformer should return correct date value`() {
        val transformer = LongToDateMappingTransformer()
        val context = mockMappingTransformerContext(123L)
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(Date(123))
    }

    @Test
    internal fun `EmptyMappingTransformer should return originalValue`() {
        val context = mockMappingTransformerContext("test")
        val result = EmptyTransformer.transform(context)
        expectThat(result)
            .isEqualTo("test")
    }

    @Test
    fun `ImplicitCollectionMappingTransformer should correctly map a collection`() {
        val shapeShift = ShapeShiftBuilder().build()
        val transformer = ImplicitCollectionMappingTransformer()
        val from = ImplicitCollectionFrom(
            listOf(ImplicitCollectionFrom.FromChild("test"), ImplicitCollectionFrom.FromChild("test2"))
        )
        val expectedResult = listOf(ImplicitCollectionTo.ToChild("test"), ImplicitCollectionTo.ToChild("test2"))
        val to = ImplicitCollectionTo()
        val context = MappingTransformerContext(
            from.fromChildren,
            from,
            to,
            from::fromChildren.javaField!!,
            to::toChildren.javaField!!,
            shapeShift
        )
        val result = transformer.transform(context)
        expectThat(result)
            .isEqualTo(expectedResult)
    }
}