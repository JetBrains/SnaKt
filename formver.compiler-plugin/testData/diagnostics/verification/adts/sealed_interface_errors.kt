// NEVER_VALIDATE
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION!>@ADT
interface NotSealed<!>

<!ADT_VIOLATION!>@ADT
sealed interface WithTypeParam<T><!>

interface SomeInterface

<!ADT_VIOLATION!>@ADT
sealed interface WithSupertype : SomeInterface<!>

@ADT
sealed interface WithMember {
    <!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>fun doSomething(): Int<!>
}

@ADT
sealed interface WithProperty {
    <!ADT_VIOLATION!>val x: Int<!>
}

<!ADT_VIOLATION!>@ADT
sealed interface WithInvalidSubtype<!>
@ADT
data class ValidSubtype(val a: Int) : WithInvalidSubtype
@ADT
data class BadSubtype(<!ADT_VIOLATION!>var b: Int<!>) : WithInvalidSubtype

@ADT
sealed interface Checkout
@ADT
data object OnTheHouse : Checkout
@ADT
data class Card(val amount: Int) : Checkout
@ADT
data class Cash(val amount: Int) : Checkout

@ADT
sealed interface Option {
    @ADT data object None : Option
    @ADT data class Some(val value: Int) : Option
}

<!ADT_VIOLATION!>fun useNotSealed(x: NotSealed) {}<!>
<!ADT_VIOLATION!>fun useWithTypeParam(x: WithTypeParam<Int>) {}<!>
<!ADT_VIOLATION!>fun useWithSupertype(x: WithSupertype) {}<!>
<!ADT_VIOLATION!>fun useWithMember(x: WithMember) {}<!>
<!ADT_VIOLATION!>fun useWithProperty(x: WithProperty) {}<!>
<!ADT_VIOLATION!>fun useWithInvalidSubtype(x: WithInvalidSubtype) {}<!>
fun <!VIPER_TEXT!>useOption<!>(o: Option) {}

fun <!VIPER_TEXT!>useCashCheckout<!>(x: Cash) {}

fun <!VIPER_TEXT!>testWhenOnCheckout<!>(x: Checkout): Int = when (x) {
    is OnTheHouse -> 0
    is Card -> -(x.amount + 1)
    is Cash -> -x.amount
}
