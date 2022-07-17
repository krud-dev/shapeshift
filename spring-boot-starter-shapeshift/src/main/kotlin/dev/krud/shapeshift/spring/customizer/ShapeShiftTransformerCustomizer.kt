/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.spring.customizer

import dev.krud.shapeshift.MappingTransformerRegistration
import dev.krud.shapeshift.ShapeShiftBuilder
import dev.krud.shapeshift.spring.ShapeShiftBuilderCustomizer
import dev.krud.shapeshift.transformer.base.MappingTransformer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.GenericTypeResolver

@Configuration
class ShapeShiftTransformerCustomizer : ShapeShiftBuilderCustomizer {
    @Autowired(required = false)
    private val mappingTransformers: List<MappingTransformer<out Any, out Any>>? = null

    override fun customize(builder: ShapeShiftBuilder) {
        mappingTransformers?.forEach { mappingTransformer ->
            val types = GenericTypeResolver.resolveTypeArguments(mappingTransformer::class.java, MappingTransformer::class.java)
            val registration = MappingTransformerRegistration(
                types!![0] as Class<Any>,
                types[1] as Class<Any>,
                mappingTransformer as MappingTransformer<Any, Any>,
                false
            )
            builder.withTransformer(
                registration
            )
        }
    }
}