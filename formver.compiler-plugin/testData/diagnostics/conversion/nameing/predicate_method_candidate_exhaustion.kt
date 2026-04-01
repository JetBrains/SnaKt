class A(val x: Int)

fun <!VIPER_TEXT!>shared<!>() {}

fun <!VIPER_TEXT!>`p$shared`<!>() {}

fun <!VIPER_TEXT!>`A$p$shared`<!>() {}

fun <!VIPER_TEXT!>`c$A$p$shared`<!>() {}

fun <!VIPER_TEXT!>predicateMethodCandidateExhaustion<!>(a: A) {
    shared()
    `p$shared`()
    `A$p$shared`()
    `c$A$p$shared`()
    a.x
}
