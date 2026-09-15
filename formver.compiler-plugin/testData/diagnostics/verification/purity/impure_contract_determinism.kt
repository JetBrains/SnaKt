// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@NeverVerify
fun <!VERIFICATION_SKIPPED!>impureSpecificationA<!>() {
    var value = 0
    verify(<!PURITY_VIOLATION!>value++ == 0<!>)
}

@NeverVerify
fun <!VERIFICATION_SKIPPED!>impureSpecificationB<!>() {
    var value = 0
    verify((<!PURITY_VIOLATION!>value++ == 0<!>))
}
