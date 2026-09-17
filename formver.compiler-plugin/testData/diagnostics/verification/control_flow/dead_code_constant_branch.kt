// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
@Suppress("UNUSED_EXPRESSION")
fun <!VIPER_TEXT!>deadTrueBranch<!>() {
    if (true) Unit else 1.0
}

@AlwaysVerify
@Suppress("UNUSED_EXPRESSION")
fun <!VIPER_TEXT!>deadFalseBranch<!>() {
    if (false) 1.0 else Unit
}

@AlwaysVerify
fun <!VIPER_TEXT!>liveAfterConstantBranch<!>() {
    if (true) Unit else {
        verify(0 == 1)
        Unit
    }
    verify(<!VIPER_VERIFICATION_ERROR!>0 == 1<!>)
}
