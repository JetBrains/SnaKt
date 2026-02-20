// FULL_VIPER_DUMP
// NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.Unique


class X(val a: Int, var b: Int)


fun <!VIPER_TEXT!>testFieldAccessShared<!>(x: X) {
    val a = x.a
    var b = x.b
}