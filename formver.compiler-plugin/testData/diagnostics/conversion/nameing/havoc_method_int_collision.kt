// NEVER_VALIDATE
// FULL_VIPER_DUMP

class A(var int: Int)

fun <!VIPER_TEXT!>`havoc$Int`<!>() {}

fun <!VIPER_TEXT!>havocMethodIntCollision<!>(a: A) {
    val local = a.int
    `havoc$Int`()
    local
}
