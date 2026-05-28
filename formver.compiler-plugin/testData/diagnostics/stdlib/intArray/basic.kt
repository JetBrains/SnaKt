// FULL_JDK
// USE_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*



fun main() {
    val list = IntArray(3)
    list.set(0, 3)
    list.set(1, 1)
    list.set(2, 2)

    selectionSort(list)

    val res = list.get(0) <= list.get(1) && list.get(1) <= list.get(2)
    verify(res)
    val res2 = list.get(0) == 1
    verify(res2)

}

//
//fun <!VIPER_TEXT!>binarySearch<!>(@Unique @Borrowed list: IntArray, target: Int): Int? {
//    preconditions {
//        // Essential: Binary search requires the input array to be sorted
//        forAll<Int, Int> { i1, i2 ->
//            (0 <= i1 && i1 <= i2 && i2 < list.size) implies (list.get(i1) <= list.get(i2))
//        }
//    }
//    postconditions<Int?> { ret ->
//
//        (ret != null) implies (0 <= ret && ret < list.size)
//
//        // 1. If an index is returned, it must contain the target value
//        ((ret != null) implies (list.get(<!ARGUMENT_TYPE_MISMATCH!>ret<!>) == target)) &&
//
//        ((ret == null) implies forAll<Int> { index ->
//            (0 <= index && index < list.size) implies (list.get(index) != target)
//        })
//    }
//
//    var low = 0
//    var high = list.size - 1
//
//    while (low <= high) {
//        loopInvariants {
//
//            forAll<Int> { i1 ->
//                forAll<Int> { i2 ->
//                    (0 <= i1 && i1 <= i2 && i2 < list.size) implies (list.get(i1) <= list.get(i2))
//                }
//            }
//
//            // Ensure pointers stay within valid mathematical thresholds
//            0 <= low && high < list.size && low <= high + 1
//
//            // Invariant: The target value does not exist to the left of 'low'
//            forAll<Int> { k ->
//                (0 <= k && k < low) implies (list.get(k) != target)
//            }
//
//            // Invariant: The target value does not exist to the right of 'high'
//            forAll<Int> { k ->
//                (high < k && k < list.size) implies (list.get(k) != target)
//            }
//        }
//
//        val mid = low + (high - low) / 2
//        val midVal = list.get(mid)
//
//        if (midVal == target) {
//            return mid
//        } else if (midVal < target) {
//            // Since midVal < target and the array is sorted,
//            // target cannot be at 'mid' or anywhere to its left.
//            low = mid + 1
//        } else {
//            // Since midVal > target and the array is sorted,
//            // target cannot be at 'mid' or anywhere to its right.
//            high = mid - 1
//        }
//    }
//
//    return null
//}
//
@NeverConvert
fun selectionSort(@Unique @Borrowed list: IntArray) {
    preconditions {
        list.size >= 0
    }
    postconditions<Unit> {
        // The array is sorted in non-decreasing order at the end of execution
        forAll<Int> { i1 ->
            triggers(list.get(i1))
            forAll<Int> { i2 ->
                triggers(list.get(i2))
                (0 <= i1 && i1 <= i2 && i2 < list.size) implies (list.get(i1) <= list.get(i2))
            }
        }
        toMultiset(list, 0, list.size) == old(toMultiset(list, 0, list.size))
    }

    var i = 0
    val n = list.size
    while (i < n) {
        loopInvariants {
            0 <= i && i <= n

            // 1. The prefix from 0 to i-1 is completely sorted
            forAll<Int> { k ->
                (0 <= k && k < i - 1) implies (list.get(k) <= list.get(k + 1))
            }

            // 2. Every element in the prefix (0..i-1) is <= every element in the suffix (i..n-1)
            forAll<Int> { k1 ->
                forAll<Int> { k2 ->
                    (0 <= k1 && k1 < i && i <= k2 && k2 < n) implies (list.get(k1) <= list.get(k2))
                }
            }
        }

        var minIndex = i
        var j = i + 1
        while (j < n) {
            loopInvariants {
                i <= minIndex && minIndex < j && j <= n

                // 3. list.get(minIndex) holds the minimum value found so far in the sub-range [i..j-1]
                forAll<Int> { k ->
                    (i <= k && k < j) implies (list.get(minIndex) <= list.get(k))
                }
            }

            if (list.get(j) < list.get(minIndex)) {
                minIndex = j
            }
            j = j + 1
        }

        // Swap the found minimum element with the element at index i
        val temp = list.get(i)
        list.set(i, list.get(minIndex))
        list.set(minIndex, temp)

        i = i + 1
    }
}
//
//
//@NeverConvert
//fun firstNNumbers(n: Int): IntArray {
//    preconditions {
//        n >= 0
//    }
//    postconditions<IntArray> { ret ->
//        n >= 0
//        ret.size == n
//        forAll<Int> { index ->
//            (0 <= index && index < n) implies (ret.get(index) == index)
//        }
//    }
//
//    val list = IntArray(n)
//    var i = 0
//    while (i < n) {
//        loopInvariants {
//            0 <= i && i <= n
//            forAll<Int> { index ->
//                (0 <= index && index < i) implies (list.get(index) == index)
//            }
//        }
//        list.set(i, i)
//        i = i + 1
//    }
//
//    return list
//
//
//}
