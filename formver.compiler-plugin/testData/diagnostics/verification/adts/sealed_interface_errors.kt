// NEVER_VALIDATE
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION!>@ADT
interface NotSealed<!>

<!ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface WithTypeParam<T><!>

interface SomeInterface

<!ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface WithSupertype : SomeInterface<!>

<!ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface WithMember {
    <!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>fun doSomething(): Int<!>
}<!>

<!ADT_VIOLATION!>@ADT
sealed interface WithProperty {
    <!ADT_VIOLATION!>val x: Int<!>
}<!>

<!ADT_VIOLATION!>@ADT
sealed interface WithInvalidSubtype<!>
@ADT
data class ValidSubtype(val a: Int) : WithInvalidSubtype
@ADT
data class BadSubtype(var b: Int) : WithInvalidSubtype

<!ADT_VIOLATION!>@ADT
sealed interface Checkout<!>
@ADT
data <!ADT_VIOLATION!>object OnTheHouse<!> : Checkout
<!ADT_VIOLATION!>@ADT
data class Card(val amount: Int) : Checkout<!>
<!ADT_VIOLATION, ADT_VIOLATION!>@ADT
data class Cash(val amount: Int) : Checkout<!>

<!ADT_VIOLATION!>fun useNotSealed(x: NotSealed) {}<!>
<!ADT_VIOLATION!>fun useWithTypeParam(x: WithTypeParam<Int>) {}<!>
<!ADT_VIOLATION!>fun useWithSupertype(x: WithSupertype) {}<!>
<!ADT_VIOLATION!>fun useWithMember(x: WithMember) {}<!>
<!ADT_VIOLATION!>fun useWithProperty(x: WithProperty) {}<!>
<!ADT_VIOLATION!>fun useWithInvalidSubtype(x: WithInvalidSubtype) {}<!>

<!ADT_VIOLATION!>fun useCashCheckout(x: Cash) {}<!>

<!ADT_VIOLATION!>fun testWhenOnCheckout(x: Checkout): Int = when (x) {
    is OnTheHouse -> 0
    is Card -> -(x.amount + 1)
    is Cash -> -x.amount
}<!>
