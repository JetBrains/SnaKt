// LOCALITY_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun requireLocalContextFunction(f: context(@Borrowed A) () -> Unit) {}

fun `pass global context function as local context function`(
    f: context(A) () -> Unit
) {
    requireLocalContextFunction(<!LOCALITY_CONTRACT_MISMATCH!>f<!>)
}
