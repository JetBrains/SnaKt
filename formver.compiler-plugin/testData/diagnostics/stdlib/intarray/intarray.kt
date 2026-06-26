// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>size<!>(@Unique arr: IntArray): Int {
    return arr.size
}

@AlwaysVerify
fun <!VIPER_TEXT!>getElement<!>(@Unique arr: IntArray): Int {
    return < !VIPER_VERIFICATION_ERROR!>arr[0]<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>setElement<!>(@Unique arr: IntArray) {
    <!VIPER_VERIFICATION_ERROR!>arr[0] = 42<!>
}

@AlwaysVerify
fun <!VIPER_TEXT!>createArray<!>(): IntArray {
    @Unique val arr = IntArray(5)
    verify(arr.size == 5)
    verify(arr[0] == 0)
    return arr
}

@AlwaysVerify
fun <!VIPER_TEXT!>toMultisetTest<!>(@Unique arr: IntArray) {
    preconditions {
        toMultiset(arr) == toMultiset(arr)
    }
}


@AlwaysVerify
fun <!VIPER_TEXT!>swap<!>(@Unique @Borrowed arr: IntArray) {
    preconditions {
        arr.size >= 2
    }

    postconditions<Unit> { res ->
        arr.size == old(arr.size)
        toMultiset(arr) == toMultiset(old(arr))
        arr[0] == old(arr[1])
        arr[1] == old(arr[0])
        forAll<Int> { i ->
            triggers(
                arr[i]
            )
            (2 <= i && i < arr.size) implies (arr[i] == old(arr[i]))
        }
    }

    val first = arr[0]
    val second = arr[1]

    arr[0] = second
    arr[1] = first
}

@AlwaysVerify
fun <!VIPER_TEXT!>test<!>(@Unique @Borrowed arr: IntArray) {
    preconditions {
        arr.size == 3
    }

    arr[2] = 1
    swap(arr)
    verify(arr[2] == 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>swapElements<!>(@Unique @Borrowed arr: IntArray, i: Int, j: Int) {
    preconditions {
        0 <= i && i < arr.size
        0 <= j && j < arr.size
    }

    postconditions<Unit> {
        arr.size == old(arr.size)
        toMultiset(arr) == toMultiset(old(arr))
        arr[i] == old(arr[j])
        arr[j] == old(arr[i])
        forAll<Int> { k ->
            triggers(arr[k])
            (0 <= k && k < arr.size && k != i && k != j) implies (arr[k] == old(arr[k]))
        }
    }

    val temp = arr[i]
    val temp2 = arr[j]
    arr[i] = temp2
    arr[j] = temp
}

@AlwaysVerify
fun <!VIPER_TEXT!>findMinIndexRec<!>(@Unique @Borrowed arr: IntArray, start: Int, current: Int, currentMin: Int): Int {
    preconditions {
        0 <= start && start < arr.size
        start <= current && current <= arr.size
        start <= currentMin && currentMin < arr.size

        forAll<Int> { k ->
            triggers(arr[k])
            (start <= k && k < current) implies (arr[currentMin] <= arr[k])
        }
    }
    postconditions<Int> { res ->
        start <= res && res < arr.size

        forAll<Int> { k ->
            triggers(arr[k])
            (start <= k && k < arr.size) implies (arr[res] <= arr[k])
        }

        arr.size == old(arr.size)
        forAll<Int> { k ->
            triggers(arr[k])
            (0 <= k && k < arr.size) implies (arr[k] == old(arr[k]))
        }
    }

    if (current == arr.size) {
        return currentMin
    }

    val nextMin = if (arr[current] < arr[currentMin]) current else currentMin
    return findMinIndexRec(arr, start, current + 1, nextMin)
}

@AlwaysVerify
fun <!VIPER_TEXT!>sortRec<!>(@Unique @Borrowed arr: IntArray, start: Int) {
    preconditions {
        0 <= start && start <= arr.size

        forAll<Int> { a ->
            triggers(arr[a])
            forAll<Int> { b ->
                triggers(arr[b])
                (0 <= a && a <= b && b < start) implies (arr[a] <= arr[b])
            }
        }
        forAll<Int> { a ->
            triggers(arr[a])
            forAll<Int> { b ->
                triggers(arr[b])
                (0 <= a && a < start && start <= b && b < arr.size) implies (arr[a] <= arr[b])
            }
        }
    }

    postconditions<Unit> {
        arr.size == old(arr.size)
        toMultiset(arr) == toMultiset(old(arr))

        forAll<Int> { a ->
            triggers(arr[a])
            forAll<Int> { b ->
                triggers(arr[b])
                (0 <= a && a <= b && b < arr.size) implies (arr[a] <= arr[b])
            }
        }
    }

    if (start < arr.size - 1) {
        val minIdx = findMinIndexRec(arr, start, start + 1, start)
        if (minIdx != start) {
            swapElements(arr, start, minIdx)
        }

        sortRec(arr, start + 1)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>recursiveSelectionSort<!>(@Unique @Borrowed arr: IntArray) {
    postconditions<Unit> {
        arr.size == old(arr.size)
        toMultiset(arr) == toMultiset(old(arr))

        forAll<Int> { a ->
            triggers(arr[a])
            forAll<Int> { b ->
                triggers(arr[b])
                (0 <= a && a <= b && b < arr.size) implies (arr[a] <= arr[b])
            }
        }
    }

    sortRec(arr, 0)
}
