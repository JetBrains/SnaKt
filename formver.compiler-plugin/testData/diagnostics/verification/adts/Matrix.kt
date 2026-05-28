// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class Mat2(
    val a: Int, val b: Int,
    val c: Int, val d: Int,
)

@Pure
fun <!VIPER_TEXT!>identity<!>(): Mat2 = Mat2(1, 0, 0, 1)

@Pure
fun <!VIPER_TEXT!>zeroMat<!>(): Mat2 = Mat2(0, 0, 0, 0)

@Pure
fun <!VIPER_TEXT!>transpose<!>(m: Mat2): Mat2 = Mat2(m.a, m.c, m.b, m.d)

@Pure
fun <!VIPER_TEXT!>addM<!>(x: Mat2, y: Mat2): Mat2 =
Mat2(x.a + y.a, x.b + y.b, x.c + y.c, x.d + y.d)

@Pure
fun <!VIPER_TEXT!>negM<!>(m: Mat2): Mat2 = Mat2(-m.a, -m.b, -m.c, -m.d)

@Pure
fun <!VIPER_TEXT!>subM<!>(x: Mat2, y: Mat2): Mat2 = addM(x, negM(y))

@Pure
fun <!VIPER_TEXT!>scaleM<!>(k: Int, m: Mat2): Mat2 = Mat2(k * m.a, k * m.b, k * m.c, k * m.d)

@Pure
fun <!VIPER_TEXT!>mulM<!>(x: Mat2, y: Mat2): Mat2 = Mat2(
    x.a * y.a + x.b * y.c,
    x.a * y.b + x.b * y.d,
    x.c * y.a + x.d * y.c,
    x.c * y.b + x.d * y.d,
)

@Pure
fun <!VIPER_TEXT!>adj<!>(m: Mat2): Mat2 = Mat2(m.d, -m.b, -m.c, m.a)

@Pure
fun <!VIPER_TEXT!>det<!>(m: Mat2): Int = m.a * m.d - m.b * m.c

@Pure
fun <!VIPER_TEXT!>trace<!>(m: Mat2): Int = m.a + m.d


@AlwaysVerify
fun <!VIPER_TEXT!>transposeInvolution<!>(m: Mat2) {
    postconditions<Unit> {
        transpose(transpose(m)) == m
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>identityLeftUnit<!>(m: Mat2) {
    postconditions<Unit> {
        mulM(identity(), m) == m
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>identityRightUnit<!>(m: Mat2) {
    postconditions<Unit> {
        mulM(m, identity()) == m
    }
}

// -- Could not be verified due to timeout

//@AlwaysVerify
//fun mulAssociative(x: Mat2, y: Mat2, z: Mat2) {
//    postconditions<Unit> {
//        mulM(mulM(x, y), z) == mulM(x, mulM(y, z))
//    }
//}

//@AlwaysVerify
//fun mulLeftDistributesOverAdd(x: Mat2, y: Mat2, z: Mat2) {
//    postconditions<Unit> {
//        mulM(x, addM(y, z)) == addM(mulM(x, y), mulM(x, z))
//    }
//}
//
