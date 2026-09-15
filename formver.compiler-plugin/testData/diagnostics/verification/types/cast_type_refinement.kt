// FULL_JDK

@file:Suppress("USELESS_IS_CHECK")

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

open class CastBase37
class CastLeaf37 : CastBase37()

@AlwaysVerify
fun <!VIPER_TEXT!>isBranchesRefineLocally<!>(value: CastBase37?) {
    if (value is CastLeaf37) {
        val fact = value is CastLeaf37
        verify(fact)
    } else {
        val fact = value !is CastLeaf37
        verify(fact)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>notIsBranchesRefineLocally<!>(value: CastBase37?) {
    if (value !is CastLeaf37) {
        val fact = value !is CastLeaf37
        verify(fact)
    } else {
        val fact = value is CastLeaf37
        verify(fact)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>safeCastMatchesTypeTest<!>(value: CastBase37?) {
    val cast = value as? CastLeaf37
    if (cast != null) {
        val originalIsLeaf = value is CastLeaf37
        val resultIsLeaf = cast is CastLeaf37
        verify(originalIsLeaf, resultIsLeaf)
    } else {
        val originalIsNotLeaf = value !is CastLeaf37
        verify(originalIsNotLeaf)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>successfulExplicitCastRefines<!>() {
    val value: CastBase37 = CastLeaf37()
    val cast = value as CastLeaf37
    val originalIsLeaf = value is CastLeaf37
    val resultIsLeaf = cast is CastLeaf37
    verify(originalIsLeaf, resultIsLeaf)
}

@Suppress("CAST_NEVER_SUCCEEDS")
@AlwaysVerify
fun <!VIPER_TEXT!>nonNullableExplicitCastMustThrow<!>() {
    val value: CastBase37 = CastBase37()
    value as CastLeaf37
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nullToNullableExplicitCastSucceeds<!>() {
    val value: CastBase37? = null
    val cast = value as CastLeaf37?
    verify(cast == null)
}

@Suppress("CAST_NEVER_SUCCEEDS")
@AlwaysVerify
fun <!VIPER_TEXT!>nonNullToNullableExplicitCastMustThrow<!>() {
    val value: CastBase37? = CastBase37()
    value as CastLeaf37?
    verify(<!VIPER_VERIFICATION_ERROR!>false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>wrongBranchFactIsRejected<!>(value: CastBase37?) {
    if (value is CastLeaf37) {
        val fact = value is CastLeaf37
        verify(fact)
    } else {
        val wrongFact = value is CastLeaf37
        verify(<!VIPER_VERIFICATION_ERROR!>wrongFact<!>)
    }
}
