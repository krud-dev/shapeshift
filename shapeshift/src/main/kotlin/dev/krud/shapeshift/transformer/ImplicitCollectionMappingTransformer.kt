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

import dev.krud.shapeshift.transformer.base.MappingTransformer
import dev.krud.shapeshift.transformer.base.MappingTransformerContext
import java.lang.reflect.ParameterizedType

class ImplicitCollectionMappingTransformer : MappingTransformer<Collection<Any>, Collection<Any>> {
    override fun transform(context: MappingTransformerContext<out Collection<Any>>): Collection<Any>? {
        context.originalValue ?: return null
        val type = context.toField?.genericType as? ParameterizedType ?: return null
        val collectionType = type.actualTypeArguments[0] as? Class<*> ?: return null
        val baseMapping = context.originalValue.map { context.shapeShift.map(it, collectionType) }
        return when (collectionType.kotlin) {
            List::class -> {
                baseMapping
            }
            MutableList::class -> {
                baseMapping.toMutableList()
            }
            Set::class -> {
                baseMapping.toSet()
            }
            MutableSet::class -> {
                baseMapping.toMutableSet()
            }
            else -> {
                baseMapping
            }
        }
    }
}