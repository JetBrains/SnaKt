// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun share(x: A) {}

fun shareVararg(vararg x: A) {}

fun `control pass local as shared regular argument`(x: @Borrowed A) {
    share(<!LOCALITY_VIOLATION!>x<!>)
}

fun `pass local as shared vararg argument`(x: @Borrowed A) {
    shareVararg(<!LOCALITY_VIOLATION!>x<!>)
}

fun `pass local as first of multiple shared vararg arguments`(x: @Borrowed A) {
    shareVararg(<!LOCALITY_VIOLATION!>x<!>, A())
}

fun `pass borrowed array as spread vararg argument`(xs: @Borrowed Array<A>) {
    shareVararg(*<!LOCALITY_VIOLATION!>xs<!>)
}
