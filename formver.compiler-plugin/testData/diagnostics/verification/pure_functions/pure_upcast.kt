import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure

open class Base(val value: Int)
class Sub(value: Int) : Base(value)
class Sub2(value: Int) : Base(value)

@Pure
fun <!VIPER_TEXT!>readValue<!>(b: Base): Int = b.value

// Test 1: pure function upcasts a non-nullable subtype to a supertype argument.
@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>pureUpcast<!>(s: Sub): Int = readValue(s)

// Test 2: pure function conditionally creates a subtype and upcasts in each branch.
@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>pureConditionalUpcast<!>(s1: Sub, s2: Sub2, flag: Boolean): Int =
    readValue(if (flag) s1 else s2)

// Test 3: pure function stores the upcast result in a variable, then reads it.
@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>pureUpcastViaVariable<!>(s: Sub): Int {
    val b: Base = s
    return b.value
}

// Test 4: nullable subtype passed to a nullable supertype parameter.
@Pure
fun <!VIPER_TEXT!>readValueNullable<!>(b: Base?): Int = if (b != null) b.value else -1

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>pureNullableUpcast<!>(s: Sub?): Int = readValueNullable(s)