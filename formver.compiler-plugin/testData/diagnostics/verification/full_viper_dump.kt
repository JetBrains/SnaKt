// FULL_VIPER_DUMP
// FULL_JDK
// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.DumpExpEmbeddings
import org.jetbrains.kotlin.formver.plugin.ADT

// Check class generation.
class Foo(val x: Int)

@ADT data class Pair(val a: Int, val b: Int)

@DumpExpEmbeddings
fun <!EXP_EMBEDDING, VIPER_TEXT!>f<!>() {
    val foo = Foo(0)
    val p = Pair(1, 2)
}
