// FULL_JDK

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.*

@OptIn(ExperimentalContracts::class)
@AlwaysVerify
@NeverVerify
fun <!VIPER_TEXT!>alwaysVerifyWithNeverVerify<!>(): Boolean {
    contract { returns(true) }
    return false
}

@OptIn(ExperimentalContracts::class)
@AlwaysVerify
@NeverConvert
fun alwaysVerifyWithNeverConvert(): Boolean {
    contract { returns(true) }
    return false
}

class Box(var value: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>selectedTopLevel<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

<!PURITY_VIOLATION!>@Pure
@NeverVerify
fun <!VERIFICATION_SKIPPED!>pureWithNeverVerify<!>(box: Box): Int {
    box.value += 1
    return box.value
}<!>

@Pure
@NeverConvert
fun pureWithNeverConvert(box: Box): Int {
    box.value += 1
    return box.value
}

@NeverConvert
fun excludedOuter() {
    @AlwaysVerify
    fun <!VIPER_TEXT!>selectedInner<!>() {
        verify(false)
    }

    @NeverVerify
    fun <!VIPER_TEXT!>convertedButUnverifiedInner<!>() {
        verify(false)
    }

    selectedInner()
    convertedButUnverifiedInner()
}
