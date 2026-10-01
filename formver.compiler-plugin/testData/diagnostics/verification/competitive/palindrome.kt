// FULL_JDK

// LeetCode 125, "Valid Palindrome", without case folding or skipping non-alphanumerics.
// https://leetcode.com/problems/valid-palindrome/
// The invariant covers both processed ends, so on exit only the middle index is
// left, where `it == length - 1 - it`.

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun String.<!VIPER_TEXT!>isPalindrome<!>(): Boolean {
    postconditions<Boolean> { res ->
        res implies forAll<Int> {
            (0 <= it && it < length) implies (get(it) == get(length - 1 - it))
        }
        (!res) implies exists<Int> {
            0 <= it && it < length && get(it) != get(length - 1 - it)
        }
    }

    var lo = 0
    var hi = length - 1
    while (lo < hi) {
        loopInvariants {
            0 <= lo && -1 <= hi && hi < length && lo + hi == length - 1
            forAll<Int> {
                ((0 <= it && it < lo) || (hi < it && it < length)) implies
                        (get(it) == get(length - 1 - it))
            }
        }
        if (get(lo) != get(hi)) break
        ++lo
        --hi
    }
    return lo >= hi
}
