// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class Stats(val n: Int, val sum: Int, val sumSq: Int)

@Pure
fun <!VIPER_TEXT!>empty<!>(): Stats = Stats(0, 0, 0)

@Pure
fun <!VIPER_TEXT!>fromSample<!>(x: Int): Stats = Stats(1, x, x * x)

@Pure
fun <!VIPER_TEXT!>update<!>(s: Stats, x: Int): Stats =
Stats(s.n + 1, s.sum + x, s.sumSq + x * x)

@Pure
fun <!VIPER_TEXT!>merge<!>(a: Stats, b: Stats): Stats =
Stats(a.n + b.n, a.sum + b.sum, a.sumSq + b.sumSq)

@Pure
fun <!VIPER_TEXT!>varianceNumerator<!>(s: Stats): Int = s.n * s.sumSq - s.sum * s.sum

@AlwaysVerify
fun <!VIPER_TEXT!>mergeCommutes<!>(a: Stats, b: Stats) {
    postconditions<Unit> {
        merge(a, b) == merge(b, a)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>mergeAssoc<!>(a: Stats, b: Stats, c: Stats) {
    postconditions<Unit> {
        merge(merge(a, b), c) == merge(a, merge(b, c))
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>mergeEmptyLeftUnit<!>(s: Stats) {
    postconditions<Unit> {
        merge(empty(), s) == s
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>mergeEmptyRightUnit<!>(s: Stats) {
    postconditions<Unit> {
        merge(s, empty()) == s
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>updateAsMergeSingle<!>(s: Stats, x: Int) {
    postconditions<Unit> {
        update(s, x) == merge(s, fromSample(x))
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>varianceNumNonNegEmpty<!>() {
    postconditions<Unit> {
        varianceNumerator(empty()) >= 0
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>varianceNumNonNegSingle<!>(x: Int) {
    postconditions<Unit> {
        varianceNumerator(fromSample(x)) == 0
    }
}
