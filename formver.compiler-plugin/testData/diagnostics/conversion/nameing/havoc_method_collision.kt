// NEVER_VALIDATE
// FULL_VIPER_DUMP

class B()

class A(var ref: B)

fun <!VIPER_TEXT!>havoc<!>() {}

fun <!VIPER_TEXT!>havocMethodCollision<!>(a: A) {
    val local = a.ref
    havoc()
    local
}
