// NEVER_VALIDATE
// FULL_VIPER_DUMP

class B()

class A(var ref: B)

fun <!VIPER_TEXT!>`havoc$B`<!>() {}

fun <!VIPER_TEXT!>havocMethodRefTypedCollision<!>(a: A) {
    val local = a.ref
    `havoc$B`()
    local
}
