// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun longMaximumControl(): Long = <!INTERNAL_ERROR!>9223372036854775807L<!>

@AlwaysVerify
fun longMinimumControl(): Long = -<!INTERNAL_ERROR!>9223372036854775807L<!> - 1L

@AlwaysVerify
fun narrowLongMaximum(): Int = <!INTERNAL_ERROR!>2147483647L<!>.toInt()

@AlwaysVerify
fun narrowLongOverflow(): Int = <!INTERNAL_ERROR!>2147483648L<!>.toInt()
