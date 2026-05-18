// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.*

@Pure
fun <!VIPER_TEXT!>testAnd<!>(a : Boolean, b : Boolean) : Boolean = (a and b) && (a.and(b))

@Pure
fun <!VIPER_TEXT!>testOr<!>(a : Boolean, b : Boolean) : Boolean = (a or b) || (a.or(b))

@Pure
fun <!VIPER_TEXT!>testXor<!>(a : Boolean, b: Boolean) : Boolean = a xor a.xor(b)

@Pure
fun <!VIPER_TEXT!>testNot<!>(a : Boolean) : Boolean = !(a.not())

@Pure
fun <!VIPER_TEXT!>testNegation<!>(a : Int) : Int = -a.unaryMinus()

@Pure
fun <!VIPER_TEXT!>testPlus<!>(a : Int) : Int = +a.unaryPlus()


@Pure
fun <!VIPER_TEXT!>testReminder<!>(a : Int, b: Int, c: Int) : Int {
    preconditions {
        b != 0 && c != 0
    }
    return (a % b).rem(c)
}

@Pure
fun <!VIPER_TEXT!>testCompare<!>(a : Int, b: Int) : Int = a.compareTo(b)
