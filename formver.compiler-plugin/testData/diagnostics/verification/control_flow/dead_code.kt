// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
@Suppress("UNREACHABLE_CODE")
fun <!VIPER_TEXT!>assertionAfterReturn<!>() {
    return
    verify(false)
}

@AlwaysVerify
@Suppress("UNREACHABLE_CODE")
fun <!VIPER_TEXT!>liveAssertionBeforeReturn<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
    return
    verify(true)
}

@AlwaysVerify
fun <!VIPER_TEXT!>assertionInFalseBranch<!>() {
    if (false) {
        verify(false)
    }
    verify(true)
}

@AlwaysVerify
fun <!VIPER_TEXT!>liveAssertionAfterFalseBranch<!>() {
    if (false) {
        verify(true)
    }
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>assertionInConstantElse<!>() {
    if (true) {
        verify(true)
    } else {
        verify(false)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>nonConstantElseControl<!>(condition: Boolean) {
    if (condition) {
        verify(true)
    } else {
        verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
    }
}

@AlwaysVerify
@Suppress("UNREACHABLE_CODE", "UNUSED_EXPRESSION")
fun unsupportedLiteralAfterReturn() {
    return
    <!INTERNAL_ERROR!>1.0<!>
}

@AlwaysVerify
@Suppress("UNUSED_EXPRESSION")
fun unsupportedLiteralInConstantElse() {
    if (true) {
        verify(true)
    } else {
        <!INTERNAL_ERROR!>1.0<!>
    }
}
