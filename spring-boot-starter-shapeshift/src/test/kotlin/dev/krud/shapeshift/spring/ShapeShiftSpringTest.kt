/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.spring

import dev.krud.shapeshift.ShapeShift
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import strikt.api.expectThat
import strikt.assertions.contains

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [ShapeShiftAutoConfiguration::class, TestConfiguration::class])
internal class ShapeShiftSpringTest {
    @Autowired
    private lateinit var shapeShift: ShapeShift

    @Autowired
    private lateinit var exampleDecorator: ExampleDecorator

    @Autowired
    private lateinit var exampleTransformer: ExampleTransformer

    @Test
    internal fun `context loads`() {
        println()
    }

    @Test
    internal fun `bean transformer is loaded`() {
        expectThat(shapeShift.transformerRegistrations.map { it.transformer })
            .contains(exampleTransformer)
    }

    @Test
    internal fun `bean decorator is loaded`() {
        expectThat(shapeShift.decoratorRegistrations.map { it.decorator })
            .contains(exampleDecorator)
    }
}