import org.jetbrains.kotlin.formver.plugin.ADT
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@ADT
data object Color1

@ADT
data object Color2

@ADT
data object Color3

// --- ADT parameter: type information is available in Viper ---

@AlwaysVerify
fun <!VIPER_TEXT!>takeAdtParam<!>(c: Color1) {}

// --- Two ADT parameters of the same type ---

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


// --- ADT cross-type inequality in verify (pure context) ---

@AlwaysVerify
fun <!VIPER_TEXT!>adtCrossTypeInequality<!>(c: Color1) {
    verify(<!VIPER_VERIFICATION_ERROR!>c != Color2<!>)
}

// --- ADT same-type equality of parameters (pure context) ---

@AlwaysVerify
fun <!VIPER_TEXT!>adtSameTypeParamEquality<!>(a: Color1, b: Color1) {
    verify(<!VIPER_VERIFICATION_ERROR!>a == b<!>)
}

// --- ADT in preconditions (pure expression linearization) ---

@AlwaysVerify
fun <!VIPER_TEXT!>adtPrecondition<!>(c: Color1) {
    preconditions {
        c != Color2
    }
}

// --- ADT constructor literal compared to parameter in verify ---

@AlwaysVerify
fun <!VIPER_TEXT!>adtLiteralVsParam<!>(c: Color1) {
    val local = Color1
    verify(<!VIPER_VERIFICATION_ERROR!>c == local<!>)
}
