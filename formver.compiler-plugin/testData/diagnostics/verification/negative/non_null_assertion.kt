// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

class PrimitiveProperty {
    var aProp: Int = 0
        set(v) {}
}

class Box(val field: Int) {
    val prop: Int
        get() = 1

    fun <!VIPER_TEXT!>method<!>(): Int = 2
}

// Unconstrained nullable operands do not discharge the generated non-null assertion.
fun <!VIPER_TEXT!>nonNullAssertionReceiverMightFail<!>(property: PrimitiveProperty?) {
    <!VIPER_VERIFICATION_ERROR!>property!!<!>.aProp = 1
}

fun <!VIPER_TEXT!>nonNullAssertionInitializerMightFail<!>(x: Int?) {
    val y = <!VIPER_VERIFICATION_ERROR!>x!!<!>
}

fun <!VIPER_TEXT!>nonNullAssertionMethodReceiverMightFail<!>(b: Box?) {
    <!VIPER_VERIFICATION_ERROR!>b!!<!>.method()
}

fun <!VIPER_TEXT!>nonNullAssertionGetterReceiverMightFail<!>(b: Box?): Int {
    return <!VIPER_VERIFICATION_ERROR!>b!!<!>.prop
}

fun <!VIPER_TEXT!>nonNullAssertionFieldReceiverMightFail<!>(b: Box?): Int {
    return <!VIPER_VERIFICATION_ERROR!>b!!<!>.field
}

inline fun nullableOperand(block: () -> Box?): Box? = block()

// Effects must not let an unconstrained nullable result bypass the null check.
@AlwaysVerify
fun nonNullAssertionEffectfulOperandMightFail(b: Box?): Int {
    var calls = 0
    return <!VIPER_VERIFICATION_ERROR!>nullableOperand {
        calls += 1
        b
    }!!<!>.field
}
