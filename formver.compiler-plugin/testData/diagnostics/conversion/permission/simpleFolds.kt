// NEVER_VALIDATE


import org.jetbrains.kotlin.formver.plugin.Unique

class A() {
    @Unique
    var x: Int = 2
}

class B() {
    @Unique
    var a: A = A()
}

fun <!VIPER_TEXT!>test<!>(@Unique b : B) {

    b.a.x = 3

    val l = b.a

    l.x = 4

}
