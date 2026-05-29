// FULL_JDK
// USE_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*


@Unique
fun <!VIPER_TEXT!>combine<!>(@Unique list1: IntArray, @Unique list2: IntArray) : IntArray {
    postconditions<IntArray> { res ->
        res.size == list1.size + list2.size
        forAll<Int> { i ->
            triggers(i)
            (0 <= i && i < old(list1.size)) implies (res.get(i) == old(list1.get(i)))
        }
        forAll<Int> { i ->
            triggers(i)
            (0 <= i && i < old(list2.size)) implies (res.get(i + old(list1.size)) == old(list2.get(i)))
        }
    }

    val resultArray = IntArray(list1.size + list2.size)
    var i = 0
    while (i < list1.size) {
        resultArray.set(i, list1.get(i))
    }

    while (i - list1.size < list2.size) {
        resultArray.set(i, list2.get(i - list2.size))
    }

    return resultArray

}


fun <!VIPER_TEXT!>reverse<!>(@Unique @Borrowed list: IntArray) {
    val size = list.size

    postconditions<Unit> { res ->
        list.size == old(list.size)
        forAll<Int> { i ->
            triggers(i)
            (0 <= i && i < size) implies (list.get(i) == old(list.get(size - i - 1)))
        }
    }

    var i = 0
    while (i < size / 2) {
        loopInvariants {
            list.size == old(list.size)
            0 <= i && i <= size / 2

            // 1. Elements at the front that HAVE been swapped
            forAll<Int> { j ->
                triggers(j)
                (0 <= j && j < i) implies (list.get(j) == old(list.get(size - j - 1)))
            }
            // 2. Elements in the middle that HAVE NOT been swapped yet
            forAll<Int> { j ->
                triggers(j)
                (i <= j && j < size - i) implies (list.get(j) == old(list.get(j)))
            }
            // 3. FIX: Elements at the back that HAVE been swapped
            forAll<Int> { j ->
                triggers(j)
                (size - i <= j && j < size) implies (list.get(j) == old(list.get(size - j - 1)))
            }
        }

        val temp = list.get(i)
        list.set(i, list.get(size - i - 1))
        list.set(size - i - 1, temp)
        i = i + 1
    }
}


fun <!VIPER_TEXT!>binarySearch<!>(@Unique @Borrowed list: IntArray, target: Int): Int? {
    preconditions {
        // Essential: Binary search requires the input array to be sorted
        forAll<Int> { i1 ->
            triggers(i1)
            forAll<Int> { i2 ->
                triggers(i2)
                (0 <= i1 && i1 <= i2 && i2 < list.size) implies (list.get(i1) <= list.get(i2))
            }
        }
    }
    postconditions<Int?> { ret ->

        (ret != null) implies (0 <= <!ARGUMENT_TYPE_MISMATCH!>ret<!> && ret <!UNSAFE_OPERATOR_CALL!><<!> list.size)

        // 1. If an index is returned, it must contain the target value
        ((ret != null) implies (list.get(<!ARGUMENT_TYPE_MISMATCH!>ret<!>) == target))

        ((ret == null) implies forAll<Int> { index ->
            (0 <= index && index < list.size) implies (list.get(index) != target)
        })

        forAll<Int> { i1 ->
            (0 <= i1 && i1 < list.size) implies (list.get(i1) == old(list.get(i1)))
        }
    }

    var low = 0
    var high = list.size - 1

    while (low <= high) {
        loopInvariants {

            forAll<Int> { i1 ->
                triggers(i1)
                forAll<Int> { i2 ->
                    triggers(i2)
                    (0 <= i1 && i1 <= i2 && i2 < list.size) implies (list.get(i1) <= list.get(i2))
                }
            }

            // Ensure pointers stay within valid mathematical thresholds
            0 <= low && high < list.size && low <= high + 1

            // Invariant: The target value does not exist to the left of 'low'
            forAll<Int> { k ->
                (0 <= k && k < low) implies (list.get(k) != target)
            }

            // Invariant: The target value does not exist to the right of 'high'
            forAll<Int> { k ->
                (high < k && k < list.size) implies (list.get(k) != target)
            }

            forAll<Int> { i1 ->
                (0 <= i1 && i1 < list.size) implies (list.get(i1) == old(list.get(i1)))
            }
        }

        val mid = low + (high - low) / 2
        val midVal = list.get(mid)

        if (midVal == target) {
            return mid
        } else if (midVal < target) {
            // Since midVal < target and the array is sorted,
            // target cannot be at 'mid' or anywhere to its left.
            low = mid + 1
        } else {
            // Since midVal > target and the array is sorted,
            // target cannot be at 'mid' or anywhere to its right.
            high = mid - 1
        }
    }

    return null
}


fun <!VIPER_TEXT!>minimum<!>(@Unique @Borrowed list: IntArray) : Int? {
    postconditions<Int?> { res ->
        (list.size == 0) implies (res == null)

        (list.size > 0) implies (
                forAll<Int> { i ->
                    (0 <= i && i < list.size) implies (res < !UNSAFE_OPERATOR_CALL!><=<!> old(list.get(i)))
                }
                )
        forAll<Int> { i ->
            (0 <= i && i < list.size) implies (list.get(i) == old(list.get(i)))
        }

        !(forAll<Int> { i ->
            (0 <= i && i < list.size) implies (res != old(list.get(i)))
        })
    }
    if (list.size == 0) return null
    var min = list.get(0)
    var i = 1
    while (i < list.size) {
        loopInvariants {
            list.size == old(list.size)

            i >= 1 && i <= list.size
            forAll<Int> { i1 ->
                (0 <= i1 && i1 < list.size) implies (list.get(i1) == old(list.get(i1)))
            }

            forAll<Int> { i2 ->
                (0 <= i2 && i2 < i) implies (list.get(i2) >= min)

            }
        }
        if (list.get(i) < min) {
            min = list.get(i)
        }
        i = i + 1
    }
    return min
}


fun <!VIPER_TEXT!>firstNNumbers<!>(n: Int): IntArray {
    preconditions {
        n >= 0
    }
    postconditions<IntArray> { ret ->
        n >= 0
        ret.size == n
        forAll<Int> { index ->
            (0 <= index && index < n) implies (ret.get(index) == index)
        }
    }

    val list = IntArray(n)
    var i = 0
    while (i < n) {
        loopInvariants {
            0 <= i && i <= n
            forAll<Int> { index ->
                (0 <= index && index < i) implies (list.get(index) == index)
            }
        }
        list.set(i, i)
        i = i + 1
    }

    return list


}
