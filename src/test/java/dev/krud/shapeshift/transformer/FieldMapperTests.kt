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

import dev.krud.shapeshift.FieldMapper
import dev.krud.shapeshift.annotation.DefaultMappingTarget
import dev.krud.shapeshift.annotation.MappedField
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import java.util.*

class FieldMapperTests {

    @Test
    fun `verify exception is thrown if a default transformer pair is registered twice`() {
        val fieldMapper = FieldMapper()
        val defaultTransformer = object : DateToLongTransformer() {
            override val isDefault: Boolean
                get() = true
        }

        expectThrows<IllegalStateException> {
            fieldMapper
                .registerTransformer(defaultTransformer::class.java, defaultTransformer)
            fieldMapper
                .registerTransformer(defaultTransformer::class.java, defaultTransformer)
        }
    }

    @Test
    internal fun `test implicit default transformer`() {
        val fieldMapper = FieldMapper()
        val defaultTransformer = object : DateToLongTransformer() {
            override val isDefault: Boolean
                get() = true
        }
        fieldMapper
            .registerTransformer(defaultTransformer::class.java, defaultTransformer)

        val sourceClass = ExampleSourceClass(Date(1))
        val destinationClass = ExampleDestinationClass()
        fieldMapper.processMappedFields(sourceClass, destinationClass)

        expectThat(destinationClass.dateAsLong)
            .isEqualTo(1L)
    }
}

@DefaultMappingTarget(ExampleDestinationClass::class)
private class ExampleSourceClass(
    @MappedField(mapTo = "dateAsLong")
    var date: Date
)

private class ExampleDestinationClass {
    var dateAsLong: Long? = null
}

