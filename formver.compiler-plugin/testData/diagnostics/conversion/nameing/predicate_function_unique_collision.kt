// NEVER_VALIDATE
// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.Pure

class A(val x: Int)

@Pure
fun <!VIPER_TEXT!>`p$unique`<!>(x: Int): Int = x

fun <!VIPER_TEXT!>predicateFunctionUniqueCollision<!>(a: A) {
    `p$unique`(0)
    a.x
}
