// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

@NeverVerify
fun <!VERIFICATION_SKIPPED!>test<!>() {
    var x = 42
    // Pure
    verify(true,false, 2 <= x)
    // Both impure
    verify(<!PURITY_VIOLATION!>x++<43<!>, <!PURITY_VIOLATION!>++x<43<!>)
}

@NeverVerify
fun <!VERIFICATION_SKIPPED!>testImpure<!>() {
    var x = 42
    verify(<!PURITY_VIOLATION!>++x<43<!>)
}
