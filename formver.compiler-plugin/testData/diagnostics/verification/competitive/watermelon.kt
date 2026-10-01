// FULL_JDK

// Codeforces 4A, "Watermelon": can w be split into two positive even parts?
// https://codeforces.com/problemset/problem/4/A
// Soundness is stated by the split (2, w - 2).

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>watermelon<!>(w: Int): Boolean {
    preconditions {
        1 <= w && w <= 100
    }
    postconditions<Boolean> { res ->
        res implies (w - 2 > 0 && (w - 2) % 2 == 0)
        (!res) implies forAll<Int> { a ->
            !(a > 0 && a % 2 == 0 && w - a > 0 && (w - a) % 2 == 0)
        }
    }

    return w % 2 == 0 && w > 2
}
