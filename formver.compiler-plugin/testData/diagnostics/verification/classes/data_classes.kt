// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

data class DataPair(val first: Int, val second: Int)

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
fun <!VIPER_TEXT!>dataClassStructuralEquality<!>() {
    verify(DataPair(10, 20) == DataPair(10, 20))
    verify(DataPair(10, 20) != DataPair(10, 30))
}

@AlwaysVerify
fun <!VIPER_TEXT!>dataClassCopyDefaults<!>() {
    val copied = DataPair(10, 20).copy(second = 30)
    verify(copied.first == 10, copied.second == 30)
}
