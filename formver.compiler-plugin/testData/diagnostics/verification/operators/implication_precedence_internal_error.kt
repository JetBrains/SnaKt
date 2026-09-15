// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

@NeverVerify
fun malformedImplicationPrecedence(value: Int): Int {
    postconditions<Int> { result ->
        value <!NONE_APPLICABLE!>>=<!> 0 <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>implies<!> (result > 0)
    }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>parenthesizedImplicationControl<!>(value: Int): Int {
    preconditions { value >= 0 }
    postconditions<Int> { result ->
        (value >= 0) implies (result > 0)
    }
    return value + 1
}
