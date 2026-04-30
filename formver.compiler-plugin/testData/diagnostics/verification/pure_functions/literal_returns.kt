// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

fun <!VIPER_TEXT!>emptyFunction<!>() {
    val x = emptyAnnotatedFunction()
}

@Pure
fun <!VIPER_TEXT!>emptyAnnotatedFunction<!>(): Int? { return null }

@Pure
fun <!VIPER_TEXT!>annotatedIntLitReturn<!>(): Int { return 42 }

@Pure
fun <!VIPER_TEXT!>annotatedBoolLitReturn<!>(): Boolean { return true }

@Pure
fun <!VIPER_TEXT!>annotatedCharLitReturn<!>(): Char { return 'A' }

@Pure
fun <!VIPER_TEXT!>annotatedStringLitReturn<!>(): String { return "Hello SnaKt" }

class X(val a: Int, var b: Int) {}

@Pure
fun <!VIPER_TEXT!>annotatedReferenceReturn<!>(x : X) : X {
    return x
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>verifiedAnnotatedIntLitReturn<!>(arg: Int): Int {
    preconditions {
        true
        arg >= 42
        arg <= 42
    }
    postconditions<Int> { result ->
        result == 42
        forAll<Int> {
            triggers(it == result)
            (it == result) implies (it == arg)
        }
    }
    return 42
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>verifiedAnnotatedBoolLitReturn<!>(arg: Int): Boolean {
    preconditions {
        arg <= 0
        arg >= 0
    }
    postconditions<Boolean> { result ->
        result
        arg == 0
    }
    return true
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>verifiedAnnotatedCharLitReturn<!>(arg: String): Char {
    preconditions {
        arg == "Hello SnaKt"
    }
    postconditions<Char> { result ->
        result == 'A'
    }
    return 'A'
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>verifiedAnnotatedStringLitReturn<!>(arg: Boolean): String {
    preconditions {
        arg
    }
    postconditions<String> { result ->
        result == "Hello SnaKt"
    }
    return "Hello SnaKt"
}
