// FULL_JDK

// LeetCode 704, "Binary Search": the index of target in a strictly increasing sequence, or -1.
// https://leetcode.com/problems/binary-search/

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>binarySearch<!>(values: String, target: Char): Int {
    preconditions {
        forAll<Int> { i ->
            (0 <= i && i < values.length) implies forAll<Int> { j ->
                (i < j && j < values.length) implies (values[i] < values[j])
            }
        }
    }
    postconditions<Int> { index ->
        -1 <= index && index < values.length
        (index >= 0) implies (values[index] == target)
        (index == -1) implies forAll<Int> { i ->
            (0 <= i && i < values.length) implies (values[i] != target)
        }
    }

    var low = 0
    var high = values.length
    while (low < high) {
        loopInvariants {
            0 <= low && low <= high && high <= values.length
            forAll<Int> { i ->
                (0 <= i && i < low) implies (values[i] < target)
                (high <= i && i < values.length) implies (target <= values[i])
            }
        }
        val middle = low + (high - low) / 2
        if (values[middle] < target) {
            low = middle + 1
        } else {
            high = middle
        }
    }
    return if (low < values.length && values[low] == target) low else -1
}
