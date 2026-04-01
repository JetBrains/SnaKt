// NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.*


@Pure
fun <!VIPER_TEXT!>test<!>(a: Int, b: Int) : Int {
    return a + b
}

fun <!VIPER_TEXT!>test2<!>() : Int {
    var a: Int
    var b: Int
    a = 3
    b = 4
    return a + b + test(a, b)
}
