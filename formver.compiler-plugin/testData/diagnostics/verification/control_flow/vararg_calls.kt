// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.verify

@NeverConvert
fun consume(vararg values: Int): Int = values.size

@NeverConvert
fun consumeMixed(prefix: Int, vararg values: Int): Int = prefix + values.size

@AlwaysVerify
fun <!VIPER_TEXT!>verifyVarargControls<!>() {
    verify()
    verify(true)
    verify(true, true, true)
}

@AlwaysVerify
fun <!VIPER_TEXT!>verifyVarargNegativeControl<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>callEmptyVararg<!>() {
    consume()
}

@AlwaysVerify
fun callSingletonVararg() {
    consume(<!INTERNAL_ERROR!>1<!>)
}

@AlwaysVerify
fun callMultipleVararg() {
    consume(<!INTERNAL_ERROR!>1, 2, 3<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>callMixedEmptyVararg<!>() {
    consumeMixed(10)
}

@AlwaysVerify
fun callMixedSingletonVararg() {
    consumeMixed(10, <!INTERNAL_ERROR!>1<!>)
}

@AlwaysVerify
fun callMixedMultipleVararg() {
    consumeMixed(10, <!INTERNAL_ERROR!>1, 2<!>)
}

@AlwaysVerify
fun callSpreadVararg(values: IntArray) {
    consume(<!INTERNAL_ERROR!>*values<!>)
}
