// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

// Positive control: size guard + indexed read.
@AlwaysVerify
fun <!VIPER_TEXT!>guardedListRead<!>(xs: List<Int>): Int? =
    if (xs.size == 0) null else xs[xs.size - 1]

// Boundary control: empty construction + indexed read.
@AlwaysVerify
fun <!VIPER_TEXT!>emptyListRead<!>(): Int = <!POSSIBLE_INDEX_OUT_OF_BOUND!>emptyList<Int>()[0]<!>

// Singleton control: one-element construction + indexed read.
@AlwaysVerify
fun <!VIPER_TEXT!>singletonListRead<!>(): Int = <!POSSIBLE_INDEX_OUT_OF_BOUND!>listOf(7)[0]<!>

// Pair: mutation + updated size + indexed read.
@AlwaysVerify
fun <!VIPER_TEXT!>appendThenRead<!>(xs: MutableList<Int>, value: Int): Int {
    val oldSize = xs.size
    xs.add(value)
    return xs[oldSize]
}

// Pair: mutation + upper-bound violation.
@AlwaysVerify
fun <!VIPER_TEXT!>appendThenReadPastEnd<!>(xs: MutableList<Int>, value: Int): Int {
    xs.add(value)
    return <!POSSIBLE_INDEX_OUT_OF_BOUND!>xs[xs.size]<!>
}

// Single-feature reference-array control.
@AlwaysVerify
fun <!VIPER_TEXT!>referenceArraySize<!>(xs: Array<Int>): Int = xs.size

// Pair: reference-array size guard + indexed read.
@AlwaysVerify
fun <!VIPER_TEXT!>guardedReferenceArrayRead<!>(xs: Array<Int>): Int? =
    if (xs.size == 0) null else xs[xs.size - 1]

// Boundary pair: empty reference-array construction + indexed read.
@AlwaysVerify
fun <!VIPER_TEXT!>emptyReferenceArrayRead<!>(): Int = emptyArray<Int>()[0]

// Single-feature primitive-array control.
@AlwaysVerify
fun <!VIPER_TEXT!>primitiveArraySize<!>(xs: IntArray): Int = xs.size
