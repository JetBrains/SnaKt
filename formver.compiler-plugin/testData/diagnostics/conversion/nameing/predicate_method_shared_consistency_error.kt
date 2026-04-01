class A(val x: Int)

fun <!VIPER_TEXT!>shared<!>() {}

fun <!VIPER_TEXT!>predicateMethodSharedConsistencyError<!>(a: A) {
    shared()
    a.x
}
