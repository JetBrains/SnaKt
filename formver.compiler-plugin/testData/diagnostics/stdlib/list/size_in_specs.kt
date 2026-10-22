// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun <!VIPER_TEXT!>size_in_postcondition<!>(l: List<Int>): Int {
    postconditions<Int> { res ->
        res == l.size
    }
    return l.size
}

@AlwaysVerify
fun <!VIPER_TEXT!>size_in_precondition<!>(l: List<Int>): Int {
    preconditions {
        l.size > 0
    }
    return l[0]
}
