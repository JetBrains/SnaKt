// REPLACE_STDLIB_EXTENSIONS

import org.jetbrains.kotlin.formver.plugin.*

@DumpExpEmbeddings
@Pure
fun <!VIPER_TEXT!>runBoyRun<!>(): Int {
    var x:Int = 0
    x = run {x = x + 2; x} + run {x = x + 3; x}
    return x
}

fun <!VIPER_TEXT!>runImpureBoyRun<!>(): Int {
    var x:Int = 0
    x = run {x = x + 1; x}
    return x
}