class B()

class A(var ref: B)

fun <!VIPER_TEXT!>`havoc$B`<!>() {}

fun <!VIPER_TEXT!>havocMethodRefTypedConsistencyError<!>(a: A) {
    val local = a.ref
    `havoc$B`()
    local
}
