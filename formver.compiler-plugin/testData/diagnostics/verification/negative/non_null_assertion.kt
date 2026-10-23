// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.Pure

class PrimitiveProperty {
    var aProp: Int = 0
        set(v) {}
}

class Box(val field: Int) {
    val prop: Int
        get() = 1

    fun <!VIPER_TEXT!>method<!>(): Int = 2
}

// Each non-null assertion below is applied to an unconstrained nullable parameter,
// so the generated `assert ... != null` cannot be discharged.
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

// A non-null assertion converts to an assert, which pure function bodies do not allow.
<!PURITY_VIOLATION!>@Pure
fun <!VERIFICATION_SKIPPED!>nonNullAssertionInPureFunctionMightFail<!>(x: Int?): Int {
    return x!! + 1
}<!>
