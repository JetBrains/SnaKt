// FULL_JDK

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.verify

@Pure
fun Int.<!VIPER_TEXT!>plusExtension<!>(other: Int): Int = this + other

@Pure
fun <!VIPER_TEXT!>plusOrdinary<!>(receiver: Int, other: Int): Int = receiver + other

class ExtensionScope(private val offset: Int) {
    @Pure
    fun Int.<!VIPER_TEXT!>plusOffset<!>(): Int = this + offset

    @Pure
    fun <!VIPER_TEXT!>plusOffsetOrdinary<!>(receiver: Int): Int = receiver + offset

    @AlwaysVerify
    fun <!VIPER_TEXT!>compareMemberExtensionCall<!>(value: Int) {
        verify(value.plusOffset() == plusOffsetOrdinary(value))
    }
}

@OptIn(ExperimentalContracts::class)
@Pure
fun Any?.<!VIPER_TEXT!>isPresentExtension<!>(): Boolean {
    contract {
        returns(true) implies (this@isPresentExtension != null)
    }
    return this != null
}

@OptIn(ExperimentalContracts::class)
@Pure
fun <!VIPER_TEXT!>isPresentOrdinary<!>(receiver: Any?): Boolean {
    contract {
        returns(true) implies (receiver != null)
    }
    return receiver != null
}

open class ExtensionBase
class ExtensionChild : ExtensionBase()

@Pure
fun ExtensionBase.<!VIPER_TEXT!>extensionRank<!>(): Int = 1
@Pure
fun ExtensionChild.<!VIPER_TEXT!>extensionRank<!>(): Int = 2

@Pure
fun <!VIPER_TEXT!>ordinaryRank<!>(receiver: ExtensionBase): Int = 1
@Pure
fun <!VIPER_TEXT!>ordinaryRank<!>(receiver: ExtensionChild): Int = 2

@AlwaysVerify
fun <!VIPER_TEXT!>compareTopLevelExtensionCall<!>(left: Int, right: Int) {
    verify(left.plusExtension(right) == plusOrdinary(left, right))
}

@AlwaysVerify
fun <!VIPER_TEXT!>compareNullableReceiverContracts<!>(value: Any?) {
    if (value.isPresentExtension()) {
        verify(<!SENSELESS_COMPARISON!>value != null<!>)
    }
    if (isPresentOrdinary(value)) {
        verify(<!SENSELESS_COMPARISON!>value != null<!>)
    }
    verify(!null.isPresentExtension())
    verify(!isPresentOrdinary(null))
}

@AlwaysVerify
fun <!VIPER_TEXT!>compareExtensionOverloads<!>(base: ExtensionBase, child: ExtensionChild) {
    verify(base.extensionRank() == ordinaryRank(base))
    verify(child.extensionRank() == ordinaryRank(child))
    verify(base.extensionRank() == 1)
    verify(child.extensionRank() == 2)
}
