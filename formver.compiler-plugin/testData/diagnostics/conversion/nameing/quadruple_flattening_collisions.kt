// NEVER_VALIDATE
package top.middle.bottom.deeper

class A_B_C_D(val first: Int)

class A_B_C {
    class D(val second: Boolean)
}

class A_B {
    class C_D(val third: Int)
}

class A {
    class B_C_D(val fourth: Boolean)
}

fun <!VIPER_TEXT!>quadrupleFlatteningCollisions<!>() {
    val w = A_B_C_D(0)
    val x = A_B_C.D(true)
    val y = A_B.C_D(1)
    val z = A.B_C_D(false)
    w.first
    x.second
    y.third
    z.fourth
}
