// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.NeverVerify

data class DestructuringPair(val first: Int, val second: Int)

@NeverVerify
fun destructureFirst(): Int {
    val (<!INTERNAL_ERROR!>first<!>) = DestructuringPair(10, 20)
    return first
}

@NeverVerify
fun destructureSecondBoundary(pair: DestructuringPair): Int {
    val (<!INTERNAL_ERROR!>_<!>, second) = pair
    return second
}
