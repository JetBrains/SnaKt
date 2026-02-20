// FULL_VIPER_DUMP
// NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.NeverConvert


class X(val a: Int, var b : Int) {}


fun <!VIPER_TEXT!>generateX<!>() : X {
    return X(1,2)
}

fun <!VIPER_TEXT!>testFieldAccessImpureShared<!>() {
    val a = generateX().a
    var b = generateX().b
}
