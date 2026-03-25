// ALWAYS_VALIDATE
// Probe: unique permissions with subtype parameters

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.NeverConvert

open class MutBox(var value: Int)
class ExtMutBox(value: Int, var extra: Int) : MutBox(value)

// With @Unique, can we read a var field?
fun <!VIPER_TEXT!>readUniqueVar<!>(@Unique b: MutBox): Int {
    return b.value
}

// With @Unique, can we write a var field?
fun <!VIPER_TEXT!>writeUniqueVar<!>(@Unique b: MutBox) {
    b.value = 42
}

// Can we pass @Unique ExtMutBox where @Unique MutBox is expected?
// FINDING: This fails with "insufficient permission to access MutBox$unique"
@NeverConvert
fun takeUniqueMutBox(@Unique b: MutBox): Int = b.value

fun <!VIPER_TEXT!>passUniqueSubtype<!>(@Unique e: ExtMutBox): Int {
    return <!VIPER_VERIFICATION_ERROR!>takeUniqueMutBox(e)<!>
}

// Can we write both fields of a @Unique child?
fun <!VIPER_TEXT!>writeUniqueChildFields<!>(@Unique e: ExtMutBox) {
    e.value = 1
    e.extra = 2
}
