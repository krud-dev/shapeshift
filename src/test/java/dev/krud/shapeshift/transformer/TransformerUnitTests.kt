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

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.lang.reflect.Field
import java.util.*

class TransformerUnitTests {
    @Test
    fun testCommaDelimitedStringToListTransformer() {
        val testString = "var1,var2,var3,var4"
        val expectedOutcome = Arrays.asList("var1", "var2", "var3", "var4")
        val transformer = CommaDelimitedStringToListTransformer()
        val outcome: List<String?> = transformer.transform(
            TestPojo.getField("testString"),
            TestPojo.getField("testStringList"),
            testString,
            TestPojo.INSTANCE,
            TestPojo.INSTANCE
        ) as List<String?>
        expectThat(outcome)
            .isEqualTo(expectedOutcome)
    }

    @Test
    fun testStringListToCommaDelimitedStringTransformer() {
        val testStringList = Arrays.asList("var1", "var2", "var3", "var4")
        val expectedOutcome = "var1,var2,var3,var4"
        val transformer = StringListToCommaDelimitedStringTransformer()
        val outcome = transformer.transform(
            TestPojo.getField("testStringList"),
            TestPojo.getField("testString"),
            testStringList,
            TestPojo.INSTANCE,
            TestPojo.INSTANCE
        )
        expectThat(outcome)
            .isEqualTo(expectedOutcome)
    }

    @Test
    fun testDateToLongTransformer() {
        val testDate = Date(100000)
        val expectedOutcome = testDate.time
        val transformer = DateToLongTransformer()
        val outcome = transformer.transform(
            TestPojo.getField("testDate"),
            TestPojo.getField("testLong"),
            testDate,
            TestPojo.INSTANCE,
            TestPojo.INSTANCE
        )
        expectThat(outcome)
            .isEqualTo(expectedOutcome)
    }

    @Test
    fun testLongToDateTransformer() {
        val testLong: Long = 100000
        val expectedOutcome = Date(testLong)
        val transformer = LongToDateTransformer()
        val outcome = transformer.transform(
            TestPojo.getField("testLong"),
            TestPojo.getField("testDate"),
            testLong,
            TestPojo.INSTANCE,
            TestPojo.INSTANCE
        )
        expectThat(outcome)
            .isEqualTo(expectedOutcome)
    }

    @Test
    fun testToStringTransformer() {
        val testInt = 130405
        val expectedOutcome = testInt.toString()
        val transformer = ToStringTransformer()
        val outcome = transformer.transform(
            TestPojo.getField("testInt"),
            TestPojo.getField("testString"),
            testInt,
            TestPojo.INSTANCE,
            TestPojo.INSTANCE
        )
        expectThat(outcome)
            .isEqualTo(expectedOutcome)
    }

    @Test
    fun testDefaultTransformer() {
        val outcome = DefaultTransformer().transform(
            TestPojo.getField("testLong"),
            TestPojo.getField("testLong"),
            1L,
            TestPojo.INSTANCE,
            TestPojo.INSTANCE
        )
        expectThat(outcome)
            .isEqualTo(1L)
    }

    internal class TestPojo {
        private val testString: String? = null
        private val testStringList: List<String>? = null
        private val testLong: Long? = null
        private val testDouble: Double? = null
        private val testInt: Int? = null
        private val testDate: Date? = null
        private val testEnum = TestEnum.Third

        companion object {
            val INSTANCE = TestPojo()
            fun getField(name: String?): Field {
                return try {
                    TestPojo::class.java.getDeclaredField(name)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    private enum class TestEnum {
        First,
        Second,
        Third
    }
}