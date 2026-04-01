// NEVER_VALIDATE
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.Pure

class A(val x: Int)

@Pure
fun <!VIPER_TEXT!>shared<!>(x: Int): Int = x

fun <!VIPER_TEXT!>predicateFunctionSharedCollision<!>(a: A) {
    shared(0)
    a.x
}
