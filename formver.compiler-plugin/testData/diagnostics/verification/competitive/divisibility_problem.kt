// FULL_JDK

// Codeforces 1328A, "Divisibility Problem": the least k >= 0 such that b divides a + k.
// https://codeforces.com/problemset/problem/1328/A
// Divisibility is stated against an explicit quotient (`divisibilityQuotient`):
// stated as `(a + res) % b == 0` over a symbolic `b`, it does not verify.

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

// Known gap: minimality does not verify; the solver does not relate `(a + k) % b` for symbolic `b` to the answer.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>divisibilityProblemMinimal<!>(a: Int, b: Int): Int {
    preconditions {
        a >= 1
        b >= 1
    }
    postconditions<Int> { res ->
        0 <= res && res < b
        forAll<Int> { k ->
            (0 <= k && k < res) implies ((a + k) % b != 0)
        }
    }

    return (b - a % b) % b
}<!>
