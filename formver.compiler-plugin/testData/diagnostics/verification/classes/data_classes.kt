// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

data class DataPair(val first: Int, val second: Int)

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

// `copy`, like the constructor, yields a fresh instance that must own its unique predicate:
// passing the result to a `@Unique` parameter requires `acc(DataPair_unique(copied), write)`.
@AlwaysVerify
fun <!VIPER_TEXT!>copyResultProvidesUniqueAccess<!>() {
    val copied = DataPair(10, 20).copy(second = 30)
    consumeUnique(copied)
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
