// NEVER_VALIDATE

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.NeverConvert

// callsInPlace contracts are currently not encoded: the contract visitor returns
// BooleanLit(true), which is filtered out by getPostconditions.  These tests
// document the generated Viper so that any future change is caught by golden-file
// comparison.

@NeverConvert
fun higherOrder(f: () -> Unit) {
    f()
}

@NeverConvert
@OptIn(ExperimentalContracts::class)
fun callsInPlaceOnce(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) }
    f()
}

// callsInPlace contract produces no Viper constraints.
// (Function object havoc behavior is already tested in function_object.kt.)
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>callWithCallsInPlace<!>(f: () -> Int): Int {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) }
    return f()
}

// Multiple invocation kinds: all produce no constraints.
@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>callAtLeastOnce<!>(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.AT_LEAST_ONCE) }
    f()
    f()
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>callAtMostOnce<!>(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.AT_MOST_ONCE) }
}
