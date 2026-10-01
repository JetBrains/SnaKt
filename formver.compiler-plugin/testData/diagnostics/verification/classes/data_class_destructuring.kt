// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.NeverVerify

data class DestructuringPair(val first: Int, val second: Int)

@NeverVerify
fun destructureFirst(): Int {
    val (<!UNSUPPORTED_FEATURE!>first<!>) = DestructuringPair(10, 20)
    return first
}

@NeverVerify
fun destructureSecondBoundary(pair: DestructuringPair): Int {
    val (<!UNSUPPORTED_FEATURE!>_<!>, second) = pair
    return second
}

@NeverVerify
fun destructureBothOnlyFirstReported(pair: DestructuringPair): Int {
    val (<!UNSUPPORTED_FEATURE!>a<!>, b) = pair
    return a
}
