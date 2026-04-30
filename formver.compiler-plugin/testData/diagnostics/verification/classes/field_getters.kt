import org.jetbrains.kotlin.formver.plugin.AlwaysVerify


class PrimitiveFields(val a: Int, var b: Int)

fun <!VIPER_TEXT!>testPrimitiveFieldGetter<!>(pf: PrimitiveFields) {
    val a = pf.a
    val b = pf.b
}

class ReferenceFields(val f: PrimitiveFields, var g: PrimitiveFields)

@AlwaysVerify
fun <!VIPER_TEXT!>testReferenceFieldGetter<!>(rf: ReferenceFields) {
    val f = rf.f
    val g = rf.g
    val fa = f.a
    val fb = f.b
    val ga = g.a
    val gb = g.b
}

@AlwaysVerify
fun <!VIPER_TEXT!>testCascadingFieldGetter<!>(rf: ReferenceFields) {
    val fa = rf.f.a
    val fb = rf.f.b
    val ga = rf.g.a
    val gb = rf.g.b
}
