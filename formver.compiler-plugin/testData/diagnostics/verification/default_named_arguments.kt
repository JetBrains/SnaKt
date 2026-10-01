// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.verify

fun <!VIPER_TEXT!>trailingDefault<!>(first: Int, second: Int = 2): Int = first + second

fun <!VIPER_TEXT!>middleDefault<!>(first: Int, second: Int = 2, third: Int): Int = first + second + third

fun <!VIPER_TEXT!>earlierParameterDefault<!>(first: Int, second: Int = first): Int = first + second

@Pure
fun <!VIPER_TEXT!>subtract<!>(first: Int, second: Int): Int = first - second

// Positive controls: explicitly expanding each default keeps the verification state consistent.
@AlwaysVerify
fun <!VIPER_TEXT!>explicitTrailingDefault<!>() {
    trailingDefault(1, 2)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>explicitMiddleDefault<!>() {
    middleDefault(first = 1, second = 2, third = 3)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>explicitEarlierParameterDefault<!>() {
    earlierParameterDefault(4, 4)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

// Omitting the same arguments emits fewer actuals than formals and aborts Silicon with a
// missing-store-entry backend failure. The test harness records no diagnostic for that abort.
@AlwaysVerify
fun <!VIPER_TEXT!>omittedTrailingDefault<!>() {
    trailingDefault(1)
    verify(false)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedMiddleDefault<!>() {
    middleDefault(first = 1, third = 3)
    verify(false)
}

@AlwaysVerify
fun <!VIPER_TEXT!>omittedEarlierParameterDefault<!>() {
    earlierParameterDefault(4)
    verify(false)
}

// A positional call is the positive control for the same values in reordered named form.
@AlwaysVerify
fun <!VIPER_TEXT!>positionalArguments<!>() {
    verify(subtract(3, 1) == 2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>reorderedNamedArguments<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>subtract(second = 1, first = 3) == 2<!>)
}
