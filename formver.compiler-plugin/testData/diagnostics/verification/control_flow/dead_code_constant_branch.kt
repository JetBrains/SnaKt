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
@Suppress("UNUSED_EXPRESSION")
fun <!VIPER_TEXT!>deadArmWidensType<!>() {
    if (true) 1 else 1.0
}

@AlwaysVerify
fun <!VIPER_TEXT!>liveAfterConstantBranch<!>() {
    if (true) {
        verify(2 == 2)
    } else {
        verify(0 == 1)
    }
    verify((if (true) 7 else 8) == 7)
    if (false) {
        verify(0 == 1)
    } else {
        verify(3 == 3)
    }
    verify((if (false) 7 else 8) == 8)
    verify(<!VIPER_VERIFICATION_ERROR!>0 == 1<!>)
}
