// ALWAYS_VALIDATE
// Probe: unique permissions across subtyping
// When a unique Child is passed where unique Parent is expected,
// do we correctly handle the unique predicate nesting?

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert

open class Box(var value: Int)
class SpecialBox(value: Int, var extra: Int) : Box(value)

// Read mutable field from subtype
fun <!VIPER_TEXT!>readMutableField<!>(b: Box): Int {
    return b.value
}

// Write mutable field on subtype
fun <!VIPER_TEXT!>writeMutableField<!>(b: Box) {
    b.value = 42
}

// Read mutable field after upcast
fun <!VIPER_TEXT!>readAfterUpcast<!>(s: SpecialBox): Int {
    val b: Box = s
    return b.value
}

// Write mutable field after upcast
fun <!VIPER_TEXT!>writeAfterUpcast<!>(s: SpecialBox) {
    val b: Box = s
    b.value = 42
}

// Write both fields through subtype
fun <!VIPER_TEXT!>writeChildFields<!>(s: SpecialBox) {
    s.value = 1
    s.extra = 2
}
