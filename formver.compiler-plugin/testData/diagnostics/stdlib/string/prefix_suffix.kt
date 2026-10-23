// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

// `false` stays unprovable after a call that omits `ignoreCase`.
@AlwaysVerify
fun <!VIPER_TEXT!>startsWithKeepsStateConsistent<!>(a: String, b: String) {
    a.startsWith(b)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>endsWithKeepsStateConsistent<!>(a: String, b: String) {
    a.endsWith(b)
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}
