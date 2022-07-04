/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift

import dev.krud.shapeshift.condition.MappingCondition
import dev.krud.shapeshift.condition.MappingConditionContext
import dev.krud.shapeshift.resolver.annotation.DefaultMappingTarget
import dev.krud.shapeshift.resolver.annotation.MappedField
import dev.krud.shapeshift.transformer.base.MappingTransformer
import dev.krud.shapeshift.transformer.base.MappingTransformerContext
import java.util.*

class ExampleFieldTransformer : MappingTransformer<Long, Date> {
    override fun transform(context: MappingTransformerContext<out Long>): Date? {
        context.originalValue ?: return null
        return Date(context.originalValue!!)
    }
}

abstract class BaseEntity {
    @MappedField(target = BaseRO::class)
    val id: Long = 123L
}

abstract class BaseRO {
    val id: Long = 321L
}

@DefaultMappingTarget(EntityRO::class)
class Entity {
    @MappedField
    val birthDate = Date(1)
}

class EntityRO : BaseRO() {
    val birthDate = Date(2)
}

@DefaultMappingTarget(GenericTo::class)
internal class FromWithDefaultMappingTarget {
    @MappedField
    val long: Long = 1L
}

internal class FromWithNullField {
    @MappedField(target = ToWithPopulatedField::class)
    val long: Long? = null
}

internal class ToWithPopulatedField {
    val long: Long? = 1L
}

internal abstract class BaseFromWithMappedField {
    @MappedField(target = BaseTo::class)
    val baseLong: Long = 1L
}

internal class FromWithBase : BaseFromWithMappedField() {
    @MappedField(target = ToWithBase::class)
    val long: Long = 1L
}

internal class FromWithComplexPath {
    @MappedField(target = GenericTo::class, mapFrom = "grandchild.greatGrandchild.long")
    val child: Child = Child()

    class Child {
        val grandchild: Grandchild = Grandchild()

        class Grandchild {
            val greatGrandchild: GreatGrandchild = GreatGrandchild()

            class GreatGrandchild {
                val long: Long = 1L
            }
        }
    }
}

internal class FromToComplexPath {
    @MappedField(target = ToWithComplexPath::class, mapTo = "child.grandchild.greatGrandchild.long")
    val long: Long = 1L
}

@MappedField(target = GenericTo::class)
internal class FromWithInvalidTypeAnnotation

internal class FromWithoutDefinedTarget {
    @MappedField
    val long: Long = 1L
}

@MappedField(mapFrom = "long")
internal class TypeFromWithoutDefinedTarget {
    val long: Long = 1L
}

internal class FromWithNullShallowPath {
    @MappedField(target = ToWithPopulatedField::class, mapFrom = "long")
    val child: Child? = null

    class Child {
        val long: Long? = null
    }
}

internal class FromWithInvalidFromPath {
    @MappedField(target = GenericTo::class, mapFrom = "i.am.invalid")
    val long: Long = 1L
}

internal class FromWithInvalidToPath {
    @MappedField(target = GenericTo::class, mapTo = "i.am.invalid")
    val long: Long = 1L
}

internal class FromWithMultipleMappedFields {
    @MappedField(target = MultipleFieldTo::class)
    @MappedField(target = MultipleFieldTo::class, mapTo = "secondLong")
    val long: Long = 1L
}

internal class FromWithMapFromSelfQualifier {
    @MappedField(target = GenericTo::class, mapFrom = "child.long")
    val child: Child = Child()

    class Child {
        val long: Long = 1L
    }
}

@MappedField(target = GenericTo::class, mapFrom = "fromWithMapFromSelfQualifierOnType.child.long")
internal class FromWithMapFromSelfQualifierOnType {
    val child: Child = Child()

    class Child {
        val long: Long = 1L
    }
}

internal class FromWithMapToSelfQualifier {
    @MappedField(target = ToWithShallowPath::class, mapTo = "toWithShallowPath.child.long")
    val long: Long = 1L
}

@MappedField(target = ToWithShallowPath::class, mapFrom = "long", mapTo = "toWithShallowPath.child.long")
internal class FromWithMapToSelfQualifierOnType {
    val long: Long = 1L
}

internal class TransformerlessFrom {
    @MappedField(target = GenericTo::class)
    val long: Long = 1L
}

@MappedField(target = GenericTo::class, mapFrom = "long")
internal class TransformerlessTypeLevelFrom {
    val long: Long = 1L
}

internal class NoTargetFrom {
    @MappedField
    val long = 1L
}

internal class DefaultTransformerFrom {
    @MappedField(target = StringTo::class)
    val long: Long = 1L
}

internal class NameTransformerFrom {
    @MappedField(target = StringTo::class, transformerRef = "myTransformer")
    val long: Long = 1L
}

internal class TypeTransformerFrom {
    @MappedField(target = StringTo::class, transformer = LongToStringTransformer::class)
    val long: Long = 1L
}

internal class GenericFrom {
    var long: Long = 1L
}

internal class GenericTo {
    var long: Long? = null
}

internal class StringTo {
    var long: String? = null
}

internal class MultipleFieldTo {
    val long: Long? = null
    val secondLong: Long? = null
}

internal class ToWithShallowPath {
    val child: Child? = null

    class Child {
        val long: Long? = null
    }
}

internal class LongEqualsOneCondition : MappingCondition<Long> {
    override fun isValid(context: MappingConditionContext<Long>): Boolean {
        return context.originalValue == 1L
    }
}

internal class LongEqualsTwoCondition : MappingCondition<Long> {
    override fun isValid(context: MappingConditionContext<Long>): Boolean {
        return context.originalValue == 2L
    }
}

internal class FromWithTruthyCondition {
    @MappedField(target = GenericTo::class, condition = LongEqualsOneCondition::class)
    val long: Long = 1L
}

internal class ToWithFalsyCondition {
    @MappedField(target = GenericTo::class, condition = LongEqualsTwoCondition::class)
    val long: Long = 1L
}

internal class ToWithComplexPath {
    val child: Child = Child()

    class Child {
        val grandchild: Grandchild? = null

        class Grandchild {
            val greatGrandchild: GreatGrandchild? = null

            class GreatGrandchild {
                val long: Long? = null
            }
        }
    }
}

internal abstract class BaseTo {
    val baseLong: Long? = null
}

internal class ToWithBase : BaseTo() {
    val long: Long? = null
}

internal class LongToStringTransformer : MappingTransformer<Long, String> {
    override fun transform(context: MappingTransformerContext<out Long>): String? {
        return context.originalValue?.toString()
    }
}