// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions

class GenericCell<T>(var value: T) {
    fun <!VIPER_TEXT!>echo<!>(argument: T): T = argument
}

class BoundedCell<T : Number>(var value: T) {
    fun <!VIPER_TEXT!>echo<!>(argument: T): T = argument
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>distinctFieldInstantiations<!>(): Int {
    postconditions<Int> { result -> result == 7 }

    val intCell = GenericCell(7)
    val booleanCell = GenericCell(true)
    val observedBoolean = booleanCell.value
    return intCell.value
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>distinctFieldInstantiationsNegative<!>(): Int {
    postconditions<Int> { result -> result == 8 }

    val intCell = GenericCell(7)
    val booleanCell = GenericCell(true)
    val observedBoolean = booleanCell.value
    return intCell.value
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>distinctMethodInstantiations<!>(): Int {
    postconditions<Int> { result -> result == 11 }

    val intCell = GenericCell(0)
    val booleanCell = GenericCell(false)
    val observedBoolean = booleanCell.echo(true)
    return intCell.echo(11)
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>distinctMethodInstantiationsNegative<!>(): Int {
    postconditions<Int> { result -> result == 12 }

    val intCell = GenericCell(0)
    val booleanCell = GenericCell(false)
    val observedBoolean = booleanCell.echo(true)
    return intCell.echo(11)
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>nullableTypeArgument<!>(): Int? {
    postconditions<Int?> { result -> result == null }
    return GenericCell<Int?>(null).value
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>nullableTypeArgumentNegative<!>(): Int? {
    postconditions<Int?> { result -> result != null }
    return GenericCell<Int?>(null).value
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>boundedTypeParameter<!>(): Int {
    postconditions<Int> { result -> result == 21 }

    val intCell = BoundedCell(1)
    return intCell.echo(21)
}<!>

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>boundedTypeParameterNegative<!>(): Int {
    postconditions<Int> { result -> result == 22 }

    val intCell = BoundedCell(1)
    return intCell.echo(21)
}<!>
