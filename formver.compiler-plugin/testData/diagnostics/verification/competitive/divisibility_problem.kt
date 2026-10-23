// FULL_JDK

// Codeforces 1328A, "Divisibility Problem": the least k >= 0 such that b divides a + k.
// https://codeforces.com/problemset/problem/1328/A
// The contracts specify the remainder-based adjustment and, separately, an explicit
// quotient witness (`divisibilityQuotient`) for the next multiple. They do not
// directly assert `(a + res) % b == 0` or quantified minimality, or connect the helpers.
// These specifications use mathematical integers, not JVM overflow semantics.

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>divisibilityProblem<!>(a: Int, b: Int): Int {
    preconditions {
        a >= 1
        b >= 1
    }
    postconditions<Int> { res ->
        0 <= res && res < b
        (a % b == 0) implies (res == 0)
        (a % b != 0) implies (res == b - a % b)
    }

    return (b - a % b) % b
}

@AlwaysVerify
fun <!VIPER_TEXT!>divisibilityQuotient<!>(a: Int, b: Int): Int {
    preconditions {
        a >= 1
        b >= 1
    }
    postconditions<Int> { m ->
        (a % b == 0) implies (a == b * m)
        (a % b != 0) implies (a + b - a % b == b * m)
    }

    return if (a % b == 0) a / b else a / b + 1
}
