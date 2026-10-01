// FULL_JDK

// LeetCode 69, "Sqrt(x)": the largest r with r * r <= x.
// https://leetcode.com/problems/sqrtx/
// `Int` is unbounded, so this says nothing about 32-bit overflow of `mid * mid`.

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>integerSqrtLinear<!>(x: Int): Int {
    preconditions {
        x >= 0
    }
    postconditions<Int> { res ->
        res >= 0
        res * res <= x
        x < (res + 1) * (res + 1)
    }

    var r = 0
    while ((r + 1) * (r + 1) <= x) {
        loopInvariants {
            r >= 0
            r * r <= x
        }
        ++r
    }
    return r
}

// `mid` rounds up so that `lo = mid` always makes progress.
@AlwaysVerify
fun <!VIPER_TEXT!>integerSqrtBinarySearch<!>(x: Int): Int {
    preconditions {
        x >= 0
    }
    postconditions<Int> { res ->
        res >= 0
        res * res <= x
        x < (res + 1) * (res + 1)
    }

    var lo = 0
    var hi = x
    while (lo < hi) {
        loopInvariants {
            0 <= lo && lo <= hi && hi <= x
            lo * lo <= x
            x < (hi + 1) * (hi + 1)
        }
        val mid = lo + (hi - lo + 1) / 2
        if (mid * mid > x) {
            hi = mid - 1
        } else {
            lo = mid
        }
    }
    return lo
}
