// FULL_JDK
// USE_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*


fun <!VIPER_TEXT!>main<!>() {
    val list = IntArray(3)
    list.set(0, 3)
    list.set(1, 1)
    list.set(2, 2)

    selectionSort(list)

    val res = list.get(0) <= list.get(1) && list.get(1) <= list.get(2)
    verify(res)
}

@NeverConvert
fun selectionSort(@Unique @Borrowed list: IntArray) {
    preconditions {
        list.size >= 0
    }
    postconditions<Unit> {
        // The array is sorted in non-decreasing order at the end of execution
        forAll<Int> { index ->
            triggers(index)
            (0 <= index && index < list.size - 1) implies (list.get(index) <= list.get(index + 1))
        }
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


@NeverConvert
fun firstNNumbers(n: Int): IntArray {
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
