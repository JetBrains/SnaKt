// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>transitionAndReturn<!>(initial: Int) {
    var state = initial
    state += 1
    verify(state == initial + 1)
    state -= 1
    verify(state == initial)
}

@AlwaysVerify
fun <!VIPER_TEXT!>zeroDeltaTransition<!>(initial: Int) {
    var state = initial
    state += 0
    verify(state == initial)
}

@AlwaysVerify
fun <!VIPER_TEXT!>transitionPastReturnedState<!>(initial: Int) {
    var state = initial
    state += 1
    state -= 1
    state -= 1
    verify(<!VIPER_VERIFICATION_ERROR!>state == initial<!>)
}
