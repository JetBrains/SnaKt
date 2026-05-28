// FULL_JDK
// FULL_VIPER_DUMP

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION, ADT_VIOLATION!>@ADT
sealed interface Shape<!>

<!ADT_VIOLATION, ADT_VIOLATION!>@ADT
data class Circle(val radius: Int) : Shape<!>

@ADT
data <!ADT_VIOLATION, ADT_VIOLATION!>object Origin<!> : Shape

<!ADT_VIOLATION!>@Pure
fun getSubtypeId(s: Shape): Int {
    return when (s) {
        is Circle -> 1
        is Origin -> 0
    }
}<!>

<!INTERNAL_ERROR!>@AlwaysVerify
fun testSubtypeId() {
    val s1 = Circle(2)
    val i1 = getSubtypeId(s1)
    verify(i1 == 1)

    val s2 = Origin
    val i2 = getSubtypeId(s2)
    verify(i2 == 0)
}<!>

<!ADT_VIOLATION!>@AlwaysVerify
fun testSubtypeIdsOnGivenType(s: Shape, c: Circle, o: Origin) {
    val i1 = getSubtypeId(s)
    verify(i1 == 0 || i1 == 1)

    val i2 = getSubtypeId(c)
    verify(i2 == 1)

    val i3 = getSubtypeId(o)
    verify(i3 == 0)
}<!>
