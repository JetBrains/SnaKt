// NEVER_VALIDATE
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.Pure

class B()

class A(var maybe: B?)

@Pure
fun <!VIPER_TEXT!>`havoc$NT$B`<!>(x: Int): Int = x

fun <!VIPER_TEXT!>havocNullableHelperNameCollision<!>(a: A) {
    val local = a.maybe
    `havoc$NT$B`(0)
    local
}
