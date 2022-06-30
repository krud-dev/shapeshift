/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.transformer.base

import dev.krud.shapeshift.util.ClassPair
import net.jodah.typetools.TypeResolver
import java.lang.reflect.Field

interface FieldTransformer<From : Any?, To : Any?> {
    fun transform(fromField: Field, toField: Field, originalValue: From?, fromObject: Any, toObject: Any): To?
    companion object {
        val FieldTransformer<*, *>.id: ClassPair get() {
            val rawArguments = TypeResolver.resolveRawArguments(FieldTransformer::class.java, this::class.java)
            return rawArguments[0] to rawArguments[1]
        }
    }
}

