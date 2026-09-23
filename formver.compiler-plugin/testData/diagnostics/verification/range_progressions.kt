// FULL_JDK
// REPLACE_STDLIB_EXTENSIONS

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>constantClosedRangeMembership<!>() {
    verify(<!PURITY_VIOLATION!>1 in 1..3<!>)
    verify(<!PURITY_VIOLATION!>3 in 1..3<!>)
    verify(<!PURITY_VIOLATION!>0 !in 1..3<!>)
    verify(<!PURITY_VIOLATION!>4 !in 1..3<!>)
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>constantUntilMembership<!>() {
    verify(<!PURITY_VIOLATION!>1 in 1 until 3<!>)
    verify(<!PURITY_VIOLATION!>2 in 1 until 3<!>)
    verify(<!PURITY_VIOLATION!>0 !in 1 until 3<!>)
    verify(<!PURITY_VIOLATION!>3 !in 1 until 3<!>)
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>parameterizedClosedRangeMembership<!>(lower: Int, value: Int, upper: Int) {
    preconditions {
        lower <= value
        value <= upper
    }
    verify(<!PURITY_VIOLATION!>value in lower..upper<!>)
}

@AlwaysVerify
fun <!VERIFICATION_SKIPPED!>parameterizedUntilMembership<!>(lower: Int, value: Int, upper: Int) {
    preconditions {
        lower <= value
        value < upper
    }
    verify(<!PURITY_VIOLATION!>value in lower until upper<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constantClosedRangeLoop<!>() {
    var sum = 0
    for (i in 1..3) {
        loopInvariants {
            0 <= sum
            sum <= 6
        }
        sum += i
    }
    verify(<!VIPER_VERIFICATION_ERROR!>sum == 6<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constantUntilLoop<!>() {
    var sum = 0
    for (i in 1 until 4) {
        loopInvariants {
            0 <= sum
            sum <= 6
        }
        sum += i
    }
    verify(<!VIPER_VERIFICATION_ERROR!>sum == 6<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constantDownToLoop<!>() {
    var sum = 0
    for (i in 3 downTo 1) {
        loopInvariants {
            0 <= sum
            sum <= 6
        }
        sum += i
    }
    verify(<!VIPER_VERIFICATION_ERROR!>sum == 6<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>constantStepLoop<!>() {
    var sum = 0
    for (i in 1..5 step 2) {
        loopInvariants {
            0 <= sum
            sum <= 9
        }
        sum += i
    }
    verify(<!VIPER_VERIFICATION_ERROR!>sum == 9<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>parameterizedClosedRangeLoop<!>(lower: Int, upper: Int) {
    preconditions { lower <= upper }
    var iterations = 0
    for (i in lower..upper) {
        loopInvariants {
            0 <= iterations
            iterations <= upper - lower + 1
        }
        iterations++
    }
    verify(<!VIPER_VERIFICATION_ERROR!>iterations == upper - lower + 1<!>)
}
