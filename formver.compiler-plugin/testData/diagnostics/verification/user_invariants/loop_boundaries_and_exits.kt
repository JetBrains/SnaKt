// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>zeroIterationsAtBoundary<!>() {
    var i = 3
    while (i < 3) {
        loopInvariants {
            i == 3
        }
        i += 1
    }
    verify(i == 3)
}

@AlwaysVerify
fun <!VIPER_TEXT!>oneIterationAtBoundary<!>() {
    var i = 2
    while (i < 3) {
        loopInvariants {
            2 <= i && i <= 3
        }
        i += 1
    }
    verify(i == 3)
}

@AlwaysVerify
fun <!VIPER_TEXT!>incrementCrossesBoundary<!>() {
    var i = 0
    while (i < 5) {
        loopInvariants {
            0 <= i && i <= 6
            i % 2 == 0
        }
        i += 2
    }
    verify(i == 6)
}

@AlwaysVerify
fun <!VIPER_TEXT!>conditionalBoundaryUpdate<!>() {
    var i = 0
    while (i < 4) {
        loopInvariants {
            0 <= i && i <= 4
            i != 3
        }
        if (i < 2) {
            i += 1
        } else {
            i += 2
        }
    }
    verify(i == 4)
}

@AlwaysVerify
fun <!VIPER_TEXT!>returnFromCountingLoop<!>(): Int {
    postconditions<Int> { result -> result == 3 }

    var i = 0
    while (true) {
        loopInvariants {
            0 <= i && i <= 3
        }
        if (i == 3) {
            return i
        }
        i += 1
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>returnFromNestedConditional<!>(stopEarly: Boolean): Int {
    postconditions<Int> { result ->
        (stopEarly implies (result == 1)) &&
            (!stopEarly implies (result == 3))
    }

    var i = 0
    while (i < 3) {
        loopInvariants {
            0 <= i && i <= 3
            stopEarly implies (i == 0)
        }
        i += 1
        if (stopEarly) {
            if (i == 1) {
                return i
            }
        }
    }
    return i
}
