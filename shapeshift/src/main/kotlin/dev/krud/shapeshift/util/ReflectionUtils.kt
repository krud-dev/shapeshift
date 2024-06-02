/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@file:JvmName("ReflectionUtils")

package dev.krud.shapeshift.util

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType


data class ClassPair<From, To>(val from: Class<out From>, val to: Class<out To>)

internal fun Field.getValue(target: Any): Any? {
    return this.get(target)
}

internal fun Field.setValue(target: Any, value: Any?) {
    this.set(target, value)
}

internal fun Class<*>.getDeclaredFieldsRecursive(): List<Field> {
    var clazz: Class<*>? = this
    val fields = mutableListOf<Field>()
    while (clazz != null) {
        fields += clazz.declaredFields
        clazz = clazz.superclass
    }

    return fields
}

internal fun Class<*>.getDeclaredFieldRecursive(name: String): Field {
    var clazz: Class<*>? = this
    while (clazz != null) {
        try {
            return clazz.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            clazz = clazz.superclass
        }
    }
    throw NoSuchFieldException(name)
}

internal fun Field.getGenericAtPosition(position: Int): Class<*> {
    if (genericType !is ParameterizedType) {
        error("Type ${this.type} is not parameterized")
    }
    val typeArgument = (genericType as ParameterizedType).actualTypeArguments[position] as Class<*>
    return typeArgument
}