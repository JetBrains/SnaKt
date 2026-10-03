// FULL_JDK

// Codeforces 41A, "Translation": is t the reverse of s?
// https://codeforces.com/problemset/problem/41/A

import org.jetbrains.kotlin.formver.plugin.*

// First index at which s disagrees with t reversed, or s.length if there is none.
fun <!VIPER_TEXT!>firstReverseMismatch<!>(s: String, t: String): Int {
    preconditions {
        s.length == t.length
    }
    postconditions<Int> { res ->
        0 <= res && res <= s.length
        forAll<Int> {
            (0 <= it && it < res) implies (s[it] == t[t.length - 1 - it])
        }
        (res != s.length) implies (s[res] != t[t.length - 1 - res])
    }

    var i = 0
    while (i < s.length) {
        loopInvariants {
            0 <= i && i <= s.length
            forAll<Int> {
                (0 <= it && it < i) implies (s[it] == t[t.length - 1 - it])
            }
        }
        if (s[i] != t[t.length - 1 - i]) break
        ++i
    }
    return i
}

@AlwaysVerify
fun <!VIPER_TEXT!>isTranslation<!>(s: String, t: String): Boolean {
    postconditions<Boolean> { res ->
        res implies (s.length == t.length && forAll<Int> {
            (0 <= it && it < s.length) implies (s[it] == t[t.length - 1 - it])
        })
        (!res) implies (s.length != t.length || exists<Int> {
            0 <= it && it < s.length && s[it] != t[t.length - 1 - it]
        })
    }

    if (s.length != t.length) return false
    return firstReverseMismatch(s, t) == s.length
}
