// NEVER_VALIDATE
// RENDER_PREDICATES

class A(val x: Int)

fun <!VIPER_TEXT!>shared<!>() {}

fun <!VIPER_TEXT!>predicateMethodSharedCollision<!>(a: A) {
    shared()
    a.x
}
