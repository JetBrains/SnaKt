// FULL_VIPER_DUMP
// NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.NeverConvert


class C(val c1: Int, val c2: Int)
class B(val b1: C, var b2: C)
class A(val a1: B, var a2: B)


fun <!VIPER_TEXT!>testFieldAccessDeepShared<!>(a: A) {
    val a1b1c1 = a.a1.b1.c1
    val a1b1c2 = a.a1.b1.c2
    val a1b2c1 = a.a1.b2.c1
    val a1b2c2 = a.a1.b2.c2
    val a2b1c1 = a.a2.b1.c1
    val a2b1c2 = a.a2.b1.c2
    val a2b2c1 = a.a2.b2.c1
    val a2b2c2 = a.a2.b2.c2

    var a1b1c1_v = a.a1.b1.c1
    var a1b1c2_v = a.a1.b1.c2
    var a1b2c1_v = a.a1.b2.c1
    var a1b2c2_v = a.a1.b2.c2
    var a2b1c1_v = a.a2.b1.c1
    var a2b1c2_v = a.a2.b1.c2
    var a2b2c1_v = a.a2.b2.c1
    var a2b2c2_v = a.a2.b2.c2
}