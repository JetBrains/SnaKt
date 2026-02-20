// FULL_VIPER_DUMP
// NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.Unique


class X(val a: Int, var b : Int) {}

@Pure
fun id(x:X) : X {
    return x
}

fun <!VIPER_TEXT!>testFieldAccessPureWithArgShared<!>(@Unique x : X) {
    val a = id(x).a
    var b = id(x).b
}