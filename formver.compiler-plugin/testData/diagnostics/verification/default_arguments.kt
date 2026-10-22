// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.verify

fun <!VIPER_TEXT!>trailingDefault<!>(a: Int, b: Int = 0): Int = a + b

@Pure
fun <!VIPER_TEXT!>pureTrailingDefault<!>(a: Int, b: Int = 0): Int = a + b

@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>inlineTrailingDefault<!>(a: Int, b: Int = 0): Int = a + b

fun <!VIPER_TEXT!>defaultInTheMiddle<!>(a: Int, skipped: Int = 0, c: Int): Int = a + skipped + c

class Boxed(val a: Int, val b: Int = 0)

// `false` stays unprovable after a call that omits a default argument.
@AlwaysVerify
fun <!VIPER_TEXT!>omittedTrailingDefault<!>() {
    trailingDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedDefaultInTheMiddle<!>() {
    defaultInTheMiddle(1, c = 2)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedInlineDefault<!>() {
    inlineTrailingDefault(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedConstructorDefault<!>() {
    Boxed(1)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

// Known gap: an omitted argument is not assumed to equal its default value.
@AlwaysVerify
fun <!VIPER_TEXT!>defaultValueIsNotAssumed<!>() {
    val defaulted = pureTrailingDefault(1)
    val given = pureTrailingDefault(1, 0)
    verify(given == 1)
    verify(<!VIPER_VERIFICATION_ERROR!>defaulted == 1<!>)
}

@Pure
fun <!VIPER_TEXT!>pureDifference<!>(a: Int, b: Int): Int = a - b

// The arguments reach the parameters they name, not the ones in their own order.
@AlwaysVerify
fun <!VIPER_TEXT!>namedArgumentsOutOfOrder<!>() {
    verify(pureDifference(b = 1, a = 3) == 2)
}

// Named arguments are evaluated in source order, whichever parameters they reach.
@AlwaysVerify
fun <!VIPER_TEXT!>namedArgumentsEvaluatedInSourceOrder<!>() {
    var x = 1
    val d = pureDifference(b = x++, a = x * 10)
    verify(d == 19)
}

@AlwaysVerify
fun <!VIPER_TEXT!>namedArgumentsOfInlineCalleeEvaluatedInSourceOrder<!>() {
    var x = 1
    val sum = inlineTrailingDefault(b = x++, a = x * 10)
    verify(sum == 21)
}
