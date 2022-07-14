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

import dev.krud.shapeshift.annotation.DefaultMappingTarget
import dev.krud.shapeshift.annotation.MappedField

@DefaultMappingTarget(AdvancedEntityDisplay::class)
data class AdvancedEntity(
    @MappedField // This field will be mapped to the "name" field in the default target class
    @MappedField(target = ReducedAdvancedEntityDisplay::class) // This field will be mapped to the "name" field in the specified target class
    val name: String,

    @MappedField(mapFrom = "childName", mapTo = "firstChildName") // This field will be mapped to the "firstChildName" field in the default target class
    @MappedField(target = ReducedAdvancedEntityDisplay::class, mapFrom = "childName", mapTo = "firstChildName") // This field will be mapped to the "firstChildName" field in the specified target class
    val firstChild: AdvancedChildEntity,

    // This field will not be mapped to ReducdedAdvancedEntityDisplay because it is missing the @MappedField annotation with the corresponding target class
    @MappedField(mapFrom = "childName", mapTo = "secondChildName") // This field will be mapped to the "secondChildName" field in the default target class
    val secondChild: AdvancedChildEntity
)