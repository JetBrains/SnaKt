// FULL_JDK
// WITH_STDLIB
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun swap(@Unique @Borrowed a: IntArray, i: Int, j: Int) {
    preconditions {
        0 <= i && i < a.size
        0 <= j && j < a.size
    }
    postconditions<Unit> {
        toMultiset(a) == old(toMultiset(a))
        a[i] == old(a[j])
        a[j] == old(a[i])
        forAll<Int> { k ->
            (0 <= k && k < a.size && k != i && k != j) implies (a[k] == old(a[k]))
        }
    }

    val tmpI = a[i]
    val tmpJ = a[j]
    a[i] = tmpJ
    a[j] = tmpI
}

@AlwaysVerify
fun insert(@Unique @Borrowed a: IntArray, currIndex: Int) {
    preconditions {
        currIndex >= 0
        currIndex < a.size
        // Precondition: The prefix up to (currIndex - 1) is globally sorted
        forAll<Int> { i ->
            forAll<Int> { j ->
                (0 <= i && i < j && j < currIndex) implies (a[i] <= a[j])
            }
        }
    }
    postconditions<Unit> {
        toMultiset(a) == old(toMultiset(a))

        // Postcondition: The prefix up to currIndex is now globally sorted
        forAll<Int> { i ->
            forAll<Int> { j ->
                (0 <= i && i < j && j <= currIndex) implies (a[i] <= a[j])
            }
        }

        // Frame condition: Unrelated elements remain untouched
        forAll<Int> { i ->
            (currIndex < i && i < a.size) implies (a[i] == old(a[i]))
        }
    }

    if (currIndex == 0) {
        return
    }

    if (a[currIndex] >= a[currIndex - 1]) {
        return
    }
    swap(a, currIndex, currIndex - 1)

    insert(a, currIndex - 1)
}

@AlwaysVerify
fun insertionSortHelper(@Unique @Borrowed a: IntArray, sortedUpTo: Int) {
    preconditions {
        sortedUpTo >= 0
        sortedUpTo <= a.size
        // Precondition: The prefix up to sortedUpTo is globally sorted
        forAll<Int> { i ->
            forAll<Int> { j ->
                (0 <= i && i < j && j < sortedUpTo) implies (a[i] <= a[j])
            }
        }
    }
    postconditions<Unit> {
        toMultiset(a) == old(toMultiset(a))

        // Postcondition: The array is completely, globally sorted
        forAll<Int> { i ->
            forAll<Int> { j ->
                (0 <= i && i < j && j < a.size) implies (a[i] <= a[j])
            }
        }
    }

    if (sortedUpTo >= a.size) {
        return
    }

    insert(a, sortedUpTo)
    insertionSortHelper(a, sortedUpTo + 1)
}

@AlwaysVerify
fun insertionSort(@Unique @Borrowed a: IntArray) {
    postconditions<Unit> {
        toMultiset(a) == old(toMultiset(a))

        // Strict Global Sortedness Postcondition
        forAll<Int> { i ->
            forAll<Int> { j ->
                (0 <= i && i < j && j < a.size) implies (a[i] <= a[j])
            }
        }
    }

    insertionSortHelper(a, 0)
}
