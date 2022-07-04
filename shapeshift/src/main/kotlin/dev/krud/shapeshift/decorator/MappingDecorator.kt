/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.decorator

import dev.krud.shapeshift.util.ClassPair
import net.jodah.typetools.TypeResolver

/**
 * Represents a decorator that can be applied to ShapeShift
 * Decorators run after mappers and contain a reference to both the from object and to object and may apply changes on both
 * or run additional actions
 * @param From The type of the from object
 * @param To The type of the from object
 **/
fun interface MappingDecorator<From : Any, To : Any> {
    /**
     * Applies the decorator to the from object and to object
     * @param context The context for the decorator
     */
    fun decorate(context: MappingDecoratorContext<From, To>)

    companion object {
        val MappingDecorator<*, *>.id: ClassPair
            get() {
                val rawArguments = TypeResolver.resolveRawArguments(MappingDecorator::class.java, this::class.java)
                return rawArguments[0] to rawArguments[1]
            }
    }
}