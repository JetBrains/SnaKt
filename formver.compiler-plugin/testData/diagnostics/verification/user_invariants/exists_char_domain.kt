// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// A quantifier variable is not introduced by a declaration, so the code range of a `Char`
// has to be conjoined as a witness constraint for anything about it to be provable.
// Preconditions, because a postcondition has no trigger to build a witness from (see exists.kt).

@AlwaysVerify
fun <!VIPER_TEXT!>someCharIsNonNegative<!>(): Boolean {
    preconditions {
        exists<Char> { it >= '\u0000' }
    }
    return true
}

@AlwaysVerify
fun <!VIPER_TEXT!>someCharIsAtMostMax<!>(): Boolean {
    preconditions {
        exists<Char> { it <= '\uFFFF' }
    }
    return true
}
