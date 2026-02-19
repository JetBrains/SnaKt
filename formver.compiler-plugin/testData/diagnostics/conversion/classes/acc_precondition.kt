// NEVER_VALIDATE


class X(var a: Any)
fun <!VIPER_TEXT!>f<!>(x: X) {
    preconditions { acc(x.a, write) }
    x.a = 123
}