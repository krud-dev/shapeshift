package dev.krud.shapeshift

import dev.krud.shapeshift.resolver.annotation.DefaultMappingTarget
import dev.krud.shapeshift.resolver.annotation.MappedField
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import java.util.*
import javax.swing.text.html.Option

class ValueContainerTest {

    internal lateinit var shapeShift: ShapeShift

    @BeforeEach
    internal fun setUp() {
        shapeShift = ShapeShiftBuilder().build()
    }

    @Test
    fun `contained values should be unwrapped, transformed and rewrapped`() {
        val toWrapped = shapeShift.map<FromWrapped, ToWrapped>(FromWrapped())
        expect {
            that(toWrapped.someField).isEqualTo(Optional.of("ABCD"))
            that(toWrapped.someField2).isEqualTo(Optional.of(123))
        }
    }

    @Test
    fun `empty but non-null container should set null`() {
        val toNullable = shapeShift.map<FromNullable, ToNullable>(FromNullable())
        println()
    }


    @DefaultMappingTarget(ToWrapped::class)
    class FromWrapped {
        @MappedField
        var someField: Optional<String>? = Optional.of("ABCD")

        @MappedField
        var someField2: Optional<String>? = Optional.of("123")
    }

    class ToWrapped {
        var someField: Optional<String>? = null
        var someField2: Optional<Int>? = null
    }

    @DefaultMappingTarget(ToNullable::class)
    class FromNullable {
        @MappedField
        var someField: Optional<String>? = Optional.empty()
        @MappedField
        var someField2: Optional<String>? = null
    }

    class ToNullable {
        var someField: String? = "ABCD"
        var someField2: String? = "ABCD"
    }
}