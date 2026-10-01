// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.verify

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
fun <!VIPER_TEXT!>extensionOverloadUsesStaticReceiverType<!>(receiver: ExtensionBase) {
    verify(receiver.extensionRank() == ordinaryRank(receiver))
    verify(<!VIPER_VERIFICATION_ERROR!>receiver.extensionRank() == 2<!>)
}
