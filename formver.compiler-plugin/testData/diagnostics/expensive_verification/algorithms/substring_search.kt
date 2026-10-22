// FULL_JDK

// LeetCode 28, "Find the Index of the First Occurrence in a String", naive search.
// https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/

import org.jetbrains.kotlin.formver.plugin.*

// `needle.length` on a full match at `start`, otherwise the first mismatching offset.
fun <!VIPER_TEXT!>matchLength<!>(haystack: String, needle: String, start: Int): Int {
    preconditions {
        0 <= start
        start + needle.length <= haystack.length
    }
    postconditions<Int> { res ->
        0 <= res && res <= needle.length
        forAll<Int> {
            (0 <= it && it < res) implies (haystack[start + it] == needle[it])
        }
        (res != needle.length) implies (haystack[start + res] != needle[res])
    }

    var j = 0
    while (j < needle.length) {
        loopInvariants {
            0 <= j && j <= needle.length
            forAll<Int> {
                (0 <= it && it < j) implies (haystack[start + it] == needle[it])
            }
        }
        if (haystack[start + j] != needle[j]) break
        ++j
    }
    return j
}

// An empty needle matches at 0.
@AlwaysVerify
fun <!VIPER_TEXT!>indexOfNaive<!>(haystack: String, needle: String): Int {
    postconditions<Int> { res ->
        -1 <= res
        (res >= 0) implies (res + needle.length <= haystack.length && forAll<Int> {
            (0 <= it && it < needle.length) implies (haystack[res + it] == needle[it])
        })
    }

    var i = 0
    while (i + needle.length <= haystack.length) {
        loopInvariants {
            0 <= i
        }
        if (matchLength(haystack, needle, i) == needle.length) break
        ++i
    }
    return if (i + needle.length <= haystack.length) i else -1
}
