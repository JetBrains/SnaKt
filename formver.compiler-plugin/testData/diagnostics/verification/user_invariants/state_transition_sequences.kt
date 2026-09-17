// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>advance<!>(state: Int): Int {
    postconditions<Int> { result -> result == state + 1 }
    return state + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>branchTransition<!>(state: Int, takeLongStep: Boolean): Int {
    postconditions<Int> { result ->
        (takeLongStep && result == state + 2) || (!takeLongStep && result == state + 1)
    }

    return when {
        takeLongStep -> state + 2
        else -> state + 1
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>countDown<!>(state: Int): Int {
    preconditions { state >= 0 }
    postconditions<Int> { result -> result == 0 }

    if (state == 0) return state
    return countDown(state - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>callBranchLoopResetSequence<!>() {
    var state = 0
    verify(state == 0)

    state = advance(state)
    verify(state == 1)

    state = branchTransition(state, true)
    verify(state == 3)

    while (state < 5) {
        loopInvariants {
            state >= 3
            state <= 5
        }
        state = advance(state)
    }
    verify(state == 5)

    state = countDown(state)
    verify(state == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedCallSequence<!>() {
    var state = advance(advance(0))
    verify(state == 2)

    state = branchTransition(state, false)
    verify(state == 3)

    state = countDown(state)
    verify(state == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>shortSequenceControl<!>() {
    var state = 0
    verify(state == 0)

    state = branchTransition(state, false)
    verify(state == 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>sequenceBoundaryFailure<!>() {
    var state = advance(0)
    verify(state == 1)

    state = advance(state)
    verify(<!VIPER_VERIFICATION_ERROR!>state == 1<!>)
}
