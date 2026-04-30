open class Foo
class Bar : Foo()

fun <!VIPER_TEXT!>smartcastReturn<!>(n: Int?): Int =
if (n != null) n else 0

fun <!VIPER_TEXT!>isNullOrEmptyWrong<!>(seq: CharSequence?): Boolean =
seq == null && seq?.length == 0

fun <!VIPER_TEXT!>isNonNullable<!>(x: Int?): Boolean {
    return x is Int
}

fun <!VIPER_TEXT!>notIsNullable<!>(x: Int?): Boolean {
    return x !is Nothing
}

fun <!VIPER_TEXT!>smartCast<!>(x: Any?): Int {
    if (x is Int) {
        return x
    } else {
        return -1
    }
}

fun <!VIPER_TEXT!>testAs<!>(foo: Foo): Bar = foo as Bar

fun <!VIPER_TEXT!>testNullableAs<!>(foo: Foo?): Bar? = foo as Bar?

fun <!VIPER_TEXT!>testSafeAs<!>(foo: Foo): Bar? = foo as? Bar

fun <!VIPER_TEXT!>testNullableSafeAs<!>(foo: Foo?): Bar? = foo as? Bar
