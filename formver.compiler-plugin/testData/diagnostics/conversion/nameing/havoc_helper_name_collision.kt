// NEVER_VALIDATE
// FULL_VIPER_DUMP

import org.jetbrains.kotlin.formver.plugin.Pure

fun <!VIPER_TEXT!>havocHelperNameCollision<!>(a: A) : Int {
    val localInt = a.int
    val res1 = `havoc`(0)
    val res2 = `havoc$Int`(0)
    val res3 = `havoc$T$Int`(7)
    return res1 + res2 + res3
}


class B()

class A(var int: Int, var ref: B)

@Pure
fun <!VIPER_TEXT!>`havoc`<!>(x: Int): Int = x + 1
@Pure
fun <!VIPER_TEXT!>`havoc$Int`<!>(x: Int): Int = x + 2
@Pure
fun <!VIPER_TEXT!>`havoc$T$Int`<!>(x: Int): Int = x + 1

