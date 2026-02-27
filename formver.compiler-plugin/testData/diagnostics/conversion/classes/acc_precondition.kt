// NEVER_VALIDATE


class X(var a: Any)
fun <!VIPER_TEXT!>f<!>(@Manual var x: X) {
    preconditions { acc(x.a, write) }
    x.a = 123
}