// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.preconditions

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

// A non-null assertion converts to an assert, which pure function bodies do not allow.
<!PURITY_VIOLATION!>@Pure
fun <!VERIFICATION_SKIPPED!>nonNullAssertionInPureFunction<!>(x: Int?): Int {
    preconditions {
        x != null
    }
    return x!! + 1
}<!>
