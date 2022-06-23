/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.dsl

import dev.krud.shapeshift.ShapeShiftBuilder
import dev.krud.shapeshift.dsl.ProgrammaticMappingResolver.Companion.withProgrammaticMapping
import dev.krud.shapeshift.dto.ResolvedMappedField
import dev.krud.shapeshift.dto.TransformerCoordinates
import dev.krud.shapeshift.resolver.MappingResolver
import dev.krud.shapeshift.transformer.base.FieldTransformer
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

class DslResult<From, To>(
    val resolvedMappedFields: List<ResolvedMappedField> = listOf()
)



@MappedFieldDsl
class DslResultBuilder<From : Any, To : Any> {
    class FieldCoordinates<RootType, LastClassType : Any, LastValueType : Any>(
        val fields: MutableList<KProperty1<LastClassType, LastValueType>> = mutableListOf()
    )

    class FieldMapping<FromValueType : Any, ToValueType : Any>(
        var fromField: FieldCoordinates<*, *, FromValueType>,
        var toField: FieldCoordinates<*, *, ToValueType>,
        var transformer: KClass<out FieldTransformer<FromValueType, ToValueType>>?
    )


    val fieldMappings = mutableListOf<FieldMapping<*, *>>()

    operator fun <Parent : Any, Child : Any, ChildValue : Any> KProperty1<Parent, Child>.rangeTo(other: KProperty1<Child, ChildValue>): FieldCoordinates<Parent, Child, ChildValue> {
        return FieldCoordinates(mutableListOf(this, other) as MutableList<KProperty1<Child, ChildValue>>)
    }

    operator fun <RootType : Any, Parent : Any, Child : Any, ChildValue : Any> FieldCoordinates<RootType, Parent, Child>.rangeTo(other: KProperty1<Child, ChildValue>): FieldCoordinates<RootType, Child, ChildValue> {
        this.fields.add(other as KProperty1<Parent, Child>)
        return this as FieldCoordinates<RootType, Child, ChildValue>
    }

    infix fun <FromClass : Any, FromValue : Any, ToClass : Any, ToValue : Any>KProperty1<FromClass, FromValue>.mappedTo(to: FieldCoordinates<To, ToClass, ToValue>): FieldMapping<FromValue, ToValue> {
        return toFieldCoordinates<From, FromClass, FromValue>().mappedTo(to)
    }

    infix fun <FromClass : Any, FromValue : Any, ToClass : Any, ToValue : Any>FieldCoordinates<From, FromClass, FromValue>.mappedTo(to: KProperty1<ToClass, ToValue>): FieldMapping<FromValue, ToValue> {
        return this.mappedTo(to.toFieldCoordinates())
    }

    infix fun <FromClass : Any, FromValue : Any, ToClass : Any, ToValue : Any>KProperty1<FromClass, FromValue>.mappedTo(to: KProperty1<ToClass, ToValue>): FieldMapping<FromValue, ToValue> {
        return this.mappedTo(to.toFieldCoordinates())
    }

    infix fun <FromClass : Any, FromValue : Any, ToClass : Any, ToValue : Any>FieldCoordinates<From, FromClass, FromValue>.mappedTo(to: FieldCoordinates<To, ToClass, ToValue>): FieldMapping<FromValue, ToValue> {
        val fieldMapping = FieldMapping(
            this,
            to,
            null
        )
        fieldMappings.add(fieldMapping)
        return fieldMapping
    }

    infix fun <FromType : Any, ToType : Any>FieldMapping<FromType, out ToType>.withTransformer(transformer: KClass<out FieldTransformer<out FromType, out ToType>>): FieldMapping<out FromType, out ToType> {
        this.transformer = transformer as KClass<Nothing>
        return this
    }

    fun build(): DslResult<From, To> {
        return DslResult(
            fieldMappings.map { fieldMapping ->
                ResolvedMappedField(
                    fieldMapping.fromField.fields.map { it.javaField!! },
                    fieldMapping.toField.fields.map { it.javaField!! },
                    if (fieldMapping.transformer == null) {
                        TransformerCoordinates.NONE
                    } else {
                        TransformerCoordinates.ofType(fieldMapping.transformer!!.java)
                    }
                )
            }
        )
    }

    private fun <RootType : Any, ClassType : Any, ValueType : Any> KProperty1<ClassType, ValueType>.toFieldCoordinates(): FieldCoordinates<RootType, ClassType, ValueType> {
        if (this is FieldCoordinates<*, *, *>) {
            return this as FieldCoordinates<RootType, ClassType, ValueType>
        }
        return FieldCoordinates(mutableListOf(this))
    }
}

fun <From : Any, To : Any> mapper(block: DslResultBuilder<From, To>.() -> Unit): DslResult<From, To> {
    val builder = DslResultBuilder<From, To>()
    builder.block()
    return builder.build()
}

class Address {
    val zip: String = "123456"
    override fun toString(): String {
        return "Address(zip='$zip')"
    }
}

class User {
    val id: Long = 0
    val address: Address = Address()
}


class UserRO {
    val id: Long = 0
    val stringId: String = "0"
    val address: Address = Address()
    val zip: String = "bla"
    override fun toString(): String {
        return "UserRO(id=$id, stringId='$stringId', address=$address, zip='$zip')"
    }
}

class StringTransformer : FieldTransformer<Long, String> {
    override val fromType: Class<Long> = Long::class.java
    override val toType: Class<String> = String::class.java

    override fun transform(fromField: Field, toField: Field, originalValue: Long?, fromObject: Any, toObject: Any): String? {
        return originalValue.toString()
    }
}

class ProgrammaticMappingResolver(
    vararg val dslResults: DslResult<*, *>
) : MappingResolver {
    override fun resolve(sourceClazz: Class<*>, targetClazz: Class<*>): List<ResolvedMappedField> {
        return dslResults.flatMap {
            it.resolvedMappedFields
        }
    }

    companion object {
        fun ShapeShiftBuilder.withProgrammaticMapping(vararg dslResults: DslResult<*, *>): ShapeShiftBuilder {
            withMappingResolver(ProgrammaticMappingResolver(*dslResults))
            return this
        }

        fun <From : Any, To : Any> ShapeShiftBuilder.withProgrammaticMapping(block: DslResultBuilder<From, To>.() -> Unit): ShapeShiftBuilder {
            val builder = DslResultBuilder<From, To>()
            builder.block()
            withMappingResolver(ProgrammaticMappingResolver(builder.build()))
            return this
        }
    }
}

//fun main() {
//    User(id, firstname, lastname, @Min(0) date)
//    {
//        firstname: "John",
//        lastname: "Doe"
//    }
//
//    // partial update
//    {
//        lastname: null
//    }
//
//
//    val shapeshift = ShapeShiftBuilder()
//        .withTransformer(StringTransformer())
//        .withProgrammaticMapping<User, UserRO> {
//            User::id mappedTo UserRO::address..Address::zip withTransformer StringTransformer::class
//            User::id mappedTo UserRO::stringId withTransformer StringTransformer::class withCondition { it > 0 }
//            User::address..Address::zip mappedTo UserRO::zip
//        }
//        .build()
//    println(shapeshift.map(User(), UserRO::class.java))
//}
//
