// FULL_JDK
// WITH_STDLIB
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>testSize<!>(@Unique @Borrowed a: IntArray) {
    val n = a.size
    verify(n == a.size)
}

@AlwaysVerify
fun <!VIPER_TEXT!>testGet<!>(@Unique @Borrowed a: IntArray) {
    preconditions {
        a.size > 0
    }
    val x = a[0]
    verify(x == a[0])
}

@AlwaysVerify
fun <!VIPER_TEXT!>testSet<!>(@Unique @Borrowed a: IntArray) {
    preconditions {
        a.size > 0
    }
    a[0] = 42
    verify(a[0] == 42)
}

@AlwaysVerify
fun <!VIPER_TEXT!>testSizePreservedAfterSet<!>(@Unique @Borrowed a: IntArray) {
    preconditions {
        a.size > 1
    }
    val before = a.size
    a[1] = 7
    verify(a.size == before)
    verify(a[1] == 7)
}

@AlwaysVerify
fun <!VIPER_TEXT!>testConstructor<!>() {
    val a = IntArray(5)
    verify(a.size == 5)
    verify(a[0] == 0)
    verify(a[4] == 0)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutate<!>(@Unique @Borrowed a: IntArray) {
    preconditions {
        a.size > 0
    }
    a[0] = 99
}

@AlwaysVerify
fun <!VIPER_TEXT!>testBorrowPreservesSize<!>(@Unique @Borrowed a: IntArray) {
    preconditions {
        a.size == 5
    }
    mutate(a)
    verify(a.size == 5)
}
