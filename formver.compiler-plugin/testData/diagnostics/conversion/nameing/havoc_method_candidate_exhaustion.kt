class B()

class A(var ref: B)

fun <!VIPER_TEXT!>havoc<!>() {}

fun <!VIPER_TEXT!>`havoc$B`<!>() {}

fun <!VIPER_TEXT!>`h$havoc$B`<!>() {}

fun <!VIPER_TEXT!>`havoc$c$B`<!>() {}

fun <!VIPER_TEXT!>`h$havoc$c$B`<!>() {}

fun <!VIPER_TEXT!>havocMethodCandidateExhaustion<!>(a: A) {
    val local = a.ref
    havoc()
    `havoc$B`()
    `h$havoc$B`()
    `havoc$c$B`()
    `h$havoc$c$B`()
    local
}
