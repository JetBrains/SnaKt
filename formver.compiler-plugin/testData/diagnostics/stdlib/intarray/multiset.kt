// FULL_JDK
// WITH_STDLIB
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>multisetReflexive<!>(@Unique @Borrowed a: IntArray) {
    verify(toMultiset(a) == toMultiset(a))
}

@AlwaysVerify
fun <!VIPER_TEXT!>multisetUnchanged<!>(@Unique @Borrowed a: IntArray) {
    verify(toMultiset(a) == old(toMultiset(a)))
}

@AlwaysVerify
fun <!VIPER_TEXT!>switchTwoElements<!>(@Unique @Borrowed a: IntArray) {
    preconditions {
        a.size == 2
    }
    postconditions<Unit> { result ->
        toMultiset(a) == old(toMultiset(a))
    }

    val tmp0 = a[0]
    val tmp1 = a[1]
    a[0] = tmp1
    a[1] = tmp0
}

@AlwaysVerify
fun <!VIPER_TEXT!>minHelper<!>(@Unique @Borrowed a: IntArray, untilIndex: Int) : Int {
    preconditions {
        a.size > 0
        untilIndex >= 0
        untilIndex < a.size
    }
    postconditions<Int> { result ->
        forAll<Int> { i ->
            (0 <= i && i <= untilIndex) implies (a[i] >= result)
        }
        forAll<Int> { i ->
            (0 <= i && i < a.size) implies (old(a[i]) == a[i])
        }
    }
    val current = a[untilIndex]
    if (untilIndex == 0) {
        return current
    } else {
        val candidate = minHelper(a, untilIndex - 1)
        if (candidate < current) {
            return candidate
        } else {
            return current
        }
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>min<!>(@Unique @Borrowed a: IntArray) : Int {
    preconditions {
        a.size > 0
    }
    postconditions<Int> { result ->
        forAll<Int> { i ->
            (0 <= i && i < a.size) implies (a[i] >= result)
        }
        forAll<Int> { i ->
            (0 <= i && i < a.size) implies (old(a[i]) == a[i])
        }
    }
    return minHelper(a, a.size - 1)


}
