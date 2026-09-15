// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>countDown<!>(n: Int): Int = if (n == 0) 0 else countDown(n - 1)

@Pure
fun <!MUTUAL_RECURSION_UNSUPPORTED, VERIFICATION_SKIPPED!>even<!>(n: Int): Boolean = n == 0 || odd(n - 1)

@Pure
fun <!MUTUAL_RECURSION_UNSUPPORTED, VERIFICATION_SKIPPED!>odd<!>(n: Int): Boolean = n != 0 && even(n - 1)

@AlwaysVerify
fun <!MUTUAL_RECURSION_UNSUPPORTED, VERIFICATION_SKIPPED!>testMutualRecursion<!>(n: Int): Boolean = even(n)
