// NEVER_VALIDATE
// FULL_VIPER_DUMP

fun main() {
    var a = A()
    var aClass = A(5)
    var b = B()
    aClass.x
    A()
    foo(5, 5)
    foo()
}

class A(val x: Int) {
    fun foo(): Int = 5
}

class B {
    var x: Int = 4
        get() {
            return 5
        }
        set(v) {
            field = v
        }
}

fun A() {
    val foo = 5

}

fun foo(x: Int, B: Int) {
    foo()
    val foo = x
    for (i in 0..10) {
        val foo = i
    }
}

fun foo() {
    val foo: Int = 5
}

