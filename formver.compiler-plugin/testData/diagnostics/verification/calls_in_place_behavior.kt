// REPLACE_STDLIB_EXTENSIONS

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.verify

// These tests document what can and cannot be verified with the current
// callsInPlace/function-object encoding.

// Inline functions work: the lambda body is substituted directly.
@NeverConvert
@Suppress("NOTHING_TO_INLINE")
inline fun inlineApply(block: () -> Int): Int = block()

@AlwaysVerify
fun <!VIPER_TEXT!>inlineLambdaIsVerifiable<!>(): Boolean {
    val result = inlineApply { 42 }
    val cond = result == 42
    verify(cond)
    return cond
}

// Inline with callsInPlace: contract is irrelevant since the body is inlined.
@NeverConvert
@OptIn(ExperimentalContracts::class)
inline fun inlineRunOnce(block: () -> Int): Int {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

@AlwaysVerify
fun <!VIPER_TEXT!>inlineCallsInPlaceVerifiable<!>(): Boolean {
    val result = inlineRunOnce { 7 + 3 }
    val cond = result == 10
    verify(cond)
    return cond
}

// Inline with captured variable: verifier can reason about captured values.
@NeverConvert
@OptIn(ExperimentalContracts::class)
inline fun inlineTransform(x: Int, block: (Int) -> Int): Int {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(x)
}

@AlwaysVerify
fun <!VIPER_TEXT!>inlineCaptureVerifiable<!>(): Boolean {
    val result = inlineTransform(5) { it + 1 }
    val cond = result == 6
    verify(cond)
    return cond
}

// Note: Non-inline function object calls cannot be tested in verification
// because passing lambda literals to non-inline functions hits a TODO in
// LambdaExp.toViperStoringIn. The conversion test (calls_in_place.kt) shows
// the havoc behavior for function parameters instead.
