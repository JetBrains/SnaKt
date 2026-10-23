// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions

class Box(val field: Int) {
    val prop: Int
        get() = 1

    fun <!VIPER_TEXT!>method<!>(): Int = 2
}

fun <!VIPER_TEXT!>nonNullAssertionInitializer<!>() {
    val x: Int? = 1
    val y = x!!
}

fun <!VIPER_TEXT!>nonNullAssertionMethodReceiver<!>() {
    val b: Box? = Box(1)
    b!!.method()
}

fun <!VIPER_TEXT!>nonNullAssertionGetterReceiver<!>(): Int {
    val b: Box? = Box(1)
    return b!!.prop
}

fun <!VIPER_TEXT!>nonNullAssertionFieldReceiver<!>(): Int {
    val b: Box? = Box(1)
    return b!!.field
}

inline fun nullableOperand(block: () -> Box?): Box? = block()

// Duplicating the operand changes both the effect count and the returned field.
// The result used after the null check must be the result of that same evaluation.
@AlwaysVerify
fun nonNullAssertionEvaluatesOperandOnce(): Int {
    postconditions<Int> { result -> result == 11 }
    var calls = 0
    val field = nullableOperand {
        calls += 1
        Box(calls)
    }!!.field
    return calls * 10 + field
}
