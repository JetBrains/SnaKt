// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
@Suppress("UNREACHABLE_CODE", "UNUSED_EXPRESSION")
fun <!VIPER_TEXT!>deadAfterReturn<!>() {
    return
    1.0
}

@AlwaysVerify
@Suppress("UNREACHABLE_CODE")
fun <!VIPER_TEXT!>liveBeforeReturn<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>0 == 1<!>)
    return
    1.0
}
