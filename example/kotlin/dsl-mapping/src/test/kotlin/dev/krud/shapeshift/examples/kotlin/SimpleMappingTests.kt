/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.examples.kotlin

import dev.krud.shapeshift.MappingStrategy
import dev.krud.shapeshift.ShapeShiftBuilder
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

internal class SimpleMappingTests {
    @Test
    internal fun `test simple mapping`() {
        val simpleEntity = SimpleEntity(
            name = "Test",
            description = "Test description",
            privateData = "Test private data"
        )
        val shapeShift = ShapeShiftBuilder()
            .withMapping<SimpleEntity, SimpleEntityDisplay> {
                SimpleEntity::name mappedTo SimpleEntityDisplay::name
                SimpleEntity::description mappedTo SimpleEntityDisplay::description
            }
            .build()

        val result: SimpleEntityDisplay = shapeShift.map(simpleEntity)

        expectThat(result.name)
            .isEqualTo("Test")
        expectThat(result.description)
            .isEqualTo("Test description")
    }

    @Test
    internal fun `test simple mapping with ad hoc transformer`() {
        val simpleEntity = SimpleEntity(
            name = "Test",
            description = "Test description",
            privateData = "Test private data"
        )
        val shapeShift = ShapeShiftBuilder()
            .withMapping<SimpleEntity, SimpleEntityDisplay> {
                SimpleEntity::name mappedTo SimpleEntityDisplay::name withTransformer { ctx ->
                    ctx.originalValue?.uppercase()
                }
            }
            .build()
        val result: SimpleEntityDisplay = shapeShift.map(simpleEntity)

        expectThat(result.name)
            .isEqualTo("TEST")
    }

    @Test
    internal fun `test simple mapping with ad hoc decorator`() {
        val simpleEntity = SimpleEntity(
            name = "Test",
            description = "Test description",
            privateData = "Test private data"
        )
        val shapeShift = ShapeShiftBuilder()
            .withMapping<SimpleEntity, SimpleEntityDisplay> {
                decorate { ctx ->
                    val (from, to) = ctx
                    to.name = "decorated"
                }
            }
            .build()
        val result: SimpleEntityDisplay = shapeShift.map(simpleEntity)

        expectThat(result.name)
            .isEqualTo("decorated")
    }

    @Test
    internal fun `test simple mapping with ad hoc condition`() {
        val simpleEntity = SimpleEntity(
            name = "Test",
            description = "Test description",
            privateData = "Test private data"
        )
        val shapeShift = ShapeShiftBuilder()
            .withMapping<SimpleEntity, SimpleEntityDisplay> {
                SimpleEntity::name mappedTo SimpleEntityDisplay::name withCondition {
                    true
                }
                SimpleEntity::description mappedTo SimpleEntityDisplay::description withCondition {
                    false
                }
            }
            .build()
        val result: SimpleEntityDisplay = shapeShift.map(simpleEntity)

        expectThat(result.name)
            .isEqualTo("Test")
        expectThat(result.description)
            .isEqualTo("")
    }

    @Test
    internal fun `test simple mapping with override mapping strategy`() {
        val simpleEntity = SimpleEntity(
            name = null,
            description = "Test description",
            privateData = "Test private data"
        )
        val shapeShift = ShapeShiftBuilder()
            .withMapping<SimpleEntity, SimpleEntityDisplay> {
                SimpleEntity::name mappedTo SimpleEntityDisplay::name overrideStrategy MappingStrategy.MAP_ALL
            }
            .build()
        val result: SimpleEntityDisplay = shapeShift.map(simpleEntity)

        expectThat(result.name)
            .isNull()
    }
}