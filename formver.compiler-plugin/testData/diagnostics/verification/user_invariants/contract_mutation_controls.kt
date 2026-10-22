// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>strictSuccessor<!>(base: Int): Int {
    postconditions<Int> { result ->
        (base >= 0) implies (result > base)
    }
    return base + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>nonStrictSuccessor<!>(base: Int): Int {
    postconditions<Int> { result ->
        (base >= 0) implies (result >= base)
    }
    return base + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>strictPostconditionPropagates<!>(base: Int) {
    preconditions { base >= 0 }
    val result = strictSuccessor(base)
    verify(result > base)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nonStrictPostconditionDoesNotProveStrictResult<!>(base: Int) {
    preconditions { base >= 0 }
    val result = nonStrictSuccessor(base)
    verify(<!VIPER_VERIFICATION_ERROR!>result > base<!>)
}
