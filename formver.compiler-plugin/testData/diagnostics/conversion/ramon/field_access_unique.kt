// FULL_VIPER_DUMP
// NEVER_VALIDATE
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.Unique

class A(val a: Int, var b: Int) {}



fun <!VIPER_TEXT!>testFieldAccessDeepUnique<!>(@Unique a: A) {

    val x = a.a
    var y = a.b


}