// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

data class DataPair(val first: Int, val second: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>constructorPropertiesPositive<!>(): Boolean {
    postconditions<Boolean> { result -> result }
    val pair = DataPair(10, 20)
    return pair.first == 10 && pair.second == 20
}

@NeverVerify
fun <!VIPER_TEXT!>constructorPropertiesBoundary<!>(pair: DataPair): Boolean {
    return pair.first == pair.second
}
