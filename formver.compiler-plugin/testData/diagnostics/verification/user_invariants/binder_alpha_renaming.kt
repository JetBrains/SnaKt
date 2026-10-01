// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>originalBinderNames<!>(value: Int): Int {
    postconditions<Int> { result ->
        result == value && forAll<Int> { candidate -> candidate * candidate >= 0 }
    }
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>renamedBinderNames<!>(value: Int): Int {
    postconditions<Int> { returnedValue ->
        returnedValue == value && forAll<Int> { integer -> integer * integer >= 0 }
    }
    return value
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>impossibleOriginalBinder<!>(value: Int): Int {
    postconditions<Int> { result -> result > value }
    return value
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>impossibleRenamedBinder<!>(value: Int): Int {
    postconditions<Int> { returnedValue -> returnedValue > value }
    return value
}<!>
