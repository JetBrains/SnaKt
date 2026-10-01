// FULL_JDK

// Codeforces 1512A, "Spy Detected!": the index of the one entry that differs from all others.
// https://codeforces.com/problemset/problem/1512/A

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>uniqueIndex<!>(values: String): Int {
    preconditions {
        3 <= values.length && values.length <= 100
        exists<Int> { unique ->
            0 <= unique && unique < values.length &&
                    forAll<Int> { i ->
                        (0 <= i && i < values.length && i != unique) implies
                                (values[i] != values[unique])
                        (0 <= i && i < values.length && i != unique) implies forAll<Int> { j ->
                            (0 <= j && j < values.length && j != unique) implies
                                    (values[i] == values[j])
                        }
                    }
        }
    }
    postconditions<Int> { unique ->
        0 <= unique && unique < values.length
        forAll<Int> { i ->
            (0 <= i && i < values.length && i != unique) implies
                    (values[i] != values[unique])
        }
    }

    // Among the first three entries, at least two carry the common value.
    val common = if (values[0] == values[1]) values[0] else values[2]
    var index = 0
    while (index < values.length && values[index] == common) {
        loopInvariants {
            0 <= index && index <= values.length
            forAll<Int> { i ->
                (0 <= i && i < index) implies (values[i] == common)
            }
            exists<Int> { unique ->
                index <= unique && unique < values.length && values[unique] != common &&
                        forAll<Int> { j ->
                            (0 <= j && j < values.length && j != unique) implies
                                    (values[j] == common)
                        }
            }
        }
        index += 1
    }
    return index
}
