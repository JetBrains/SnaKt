import org.jetbrains.kotlin.formver.plugin.ADT
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@ADT
object Color1

@ADT
object Color2

@ADT
object Color3

// --- ADT parameter: type information is available in Viper ---

@AlwaysVerify
fun <!VIPER_TEXT!>takeAdtParam<!>(c: Color1) {}

// --- Two ADT parameters of the same type ---

@AlwaysVerify
fun <!VIPER_TEXT!>twoSameTypeAdtParams<!>(a: Color1, b: Color1) {}

// --- Two ADT parameters of different types ---

@AlwaysVerify
fun <!VIPER_TEXT!>twoDifferentTypeAdtParams<!>(a: Color1, b: Color2) {}

// --- Mix of ADT and regular parameters ---

@AlwaysVerify
fun <!VIPER_TEXT!>mixedParams<!>(c: Color1, n: Int) {}

// --- ADT local variable assignment ---

@AlwaysVerify
fun <!VIPER_TEXT!>adtLocalVar<!>() {
    val c = Color1
}

// --- Same ADT value is equal to itself ---

@AlwaysVerify
fun <!VIPER_TEXT!>adtSameValueEquality<!>() {
    val a = Color1
    val b = Color1
    verify(a == b)
}

// --- All three ADT values are pairwise distinct at the type level ---
// (Silicon should prove these from the unique type functions + injection axioms)

@AlwaysVerify
fun <!VIPER_TEXT!>threeAdtParams<!>(a: Color1, b: Color2, c: Color3) {}
