// NEVER_VALIDATE
package top.middle.bottom

class A_B_C(val first: Int)

class A {
    class B_C(val second: Boolean)
}

class A_B {
    class C(val third: Int)
}

fun <!VIPER_TEXT!>multipleFlatteningCollisions<!>() {
    val x = A_B_C(0)
    val y = A.B_C(true)
    val z = A_B.C(1)
    x.first
    y.second
    z.third
}
