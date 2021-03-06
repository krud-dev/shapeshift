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
import dev.krud.shapeshift.ShapeShiftBuilder
import dev.krud.shapeshift.spring.customizer.ShapeShiftDecoratorCustomizer
import dev.krud.shapeshift.spring.customizer.ShapeShiftTransformerCustomizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnClass(ShapeShift::class)
class ShapeShiftAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ShapeShift::class)
    fun shapeShift(@Autowired(required = false) customizers: List<ShapeShiftBuilderCustomizer>?): ShapeShift {
        val builder = ShapeShiftBuilder()
        customizers?.forEach { customizer ->
            customizer.customize(builder)
        }
        return builder.build()
    }

    @Bean
    fun shapeShiftTransformerCustomizer() = ShapeShiftTransformerCustomizer()

    @Bean
    fun shapeShiftDecoratorCustomizer() = ShapeShiftDecoratorCustomizer()
}