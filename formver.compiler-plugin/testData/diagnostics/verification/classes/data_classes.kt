// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

data class DataPair(val first: Int, val second: Int) {
    @AlwaysVerify
    fun <!VIPER_TEXT!>copy<!>(tag: String): DataPair = this
}

@AlwaysVerify
fun <!VIPER_TEXT!>consumeUnique<!>(@Unique pair: DataPair) {}

@AlwaysVerify
fun <!VIPER_TEXT!>destructureDataClass<!>() {
    val (first) = DataPair(10, 20)
    verify(first == 10)
}

@AlwaysVerify
fun <!VIPER_TEXT!>dataClassComponent<!>() {
    val first = DataPair(10, 20).component1()
    verify(first == 10)
}

@AlwaysVerify
fun <!VIPER_TEXT!>dataClassCopyDefaults<!>() {
    val copied = DataPair(10, 20).copy(second = 30)
    verify(copied.first == 10, copied.second == 30)
}

// A fresh scalar-only copy grants outer ownership, not ownership of aliased child objects.
@AlwaysVerify
fun <!VIPER_TEXT!>copyResultProvidesUniqueAccess<!>() {
    val copied = DataPair(10, 20).copy(second = 30)
    consumeUnique(copied)
}

@AlwaysVerify
fun <!VIPER_TEXT!>copyArgumentOrder<!>() {
    var n = 0
    val copied = DataPair(0, 0).copy(second = ++n, first = ++n)
    verify(copied.first == 2, copied.second == 1, n == 2)
}

@AlwaysVerify
fun <!VIPER_TEXT!>copyReceiverSnapshot<!>() {
    var pair = DataPair(1, 2)
    val copied = pair.copy(second = if (true) { pair = DataPair(9, 9); 3 } else 4)
    verify(copied.first == 1, copied.second == 3, pair.first == 9)
}

@AlwaysVerify
fun <!VIPER_TEXT!>copyLookalike<!>() {
    val pair = DataPair(1, 2)
    val copied = pair.copy("tag")
    // An ordinary overload has no synthesized allocation or property postconditions.
    verify(<!VIPER_VERIFICATION_ERROR!>copied == pair<!>)
}

class CopyChild(var value: Int)
data class OwnedCopy<!UNSUPPORTED_FEATURE!>(@Unique val child: CopyChild)<!>

@AlwaysVerify
fun rejectOwnedCopy(pair: OwnedCopy) {
    pair.copy()
}

data class MutablePair(var first: Int, val second: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>destructureWithVarProperty<!>(pair: MutablePair) {
    val (first, second) = pair
    verify(second == pair.second)
}

data class NonLexicalPair(val z: Int, val a: Int) {
    @NeverConvert
    fun component01(): Int = 99
    @NeverConvert
    fun `1`(): Int = 99
}

@AlwaysVerify
fun <!VIPER_TEXT!>componentConstructorOrder<!>() {
    val pair = NonLexicalPair(10, 20)
    val (z, a) = pair
    val second = pair.component2()
    verify(z == 10, a == 20, second == 20)
}

@AlwaysVerify
fun <!VIPER_TEXT!>componentLookalikes<!>() {
    val pair = NonLexicalPair(10, 20)
    val padded = pair.component01()
    val numeric = pair.`1`()
    verify(<!VIPER_VERIFICATION_ERROR!>padded == 10 || numeric == 10<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>componentFalseValue<!>() {
    val pair = NonLexicalPair(10, 20)
    val second = pair.component2()
    verify(<!VIPER_VERIFICATION_ERROR!>second == 10<!>)
}
