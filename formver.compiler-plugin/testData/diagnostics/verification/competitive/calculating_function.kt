// FULL_JDK

// Codeforces 486A, "Calculating Function": f(n) = -1 + 2 - 3 + ... +- n.
// https://codeforces.com/problemset/problem/486/A
// The original takes n up to 10^15 (`Long`); `Int` is unbounded in verification.

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>calculatingFunctionClosedForm<!>(n: Int): Int {
    preconditions {
        n >= 1
    }
    postconditions<Int> { res ->
        (n % 2 == 0) implies (res == n / 2)
        (n % 2 != 0) implies (res == -((n + 1) / 2))
    }

    return if (n % 2 == 0) n / 2 else -((n + 1) / 2)
}

// The invariant is the closed form applied to the `i - 1` terms consumed so far.
@AlwaysVerify
fun <!VIPER_TEXT!>calculatingFunctionLoop<!>(n: Int): Int {
    preconditions {
        n >= 1
    }
    postconditions<Int> { res ->
        (n % 2 == 0) implies (res == n / 2)
        (n % 2 != 0) implies (res == -((n + 1) / 2))
    }

    var res = 0
    var i = 1
    while (i <= n) {
        loopInvariants {
            1 <= i && i <= n + 1
            ((i - 1) % 2 == 0) implies (res == (i - 1) / 2)
            ((i - 1) % 2 != 0) implies (res == -(i / 2))
        }
        if (i % 2 == 0) {
            res += i
        } else {
            res -= i
        }
        ++i
    }
    return res
}
