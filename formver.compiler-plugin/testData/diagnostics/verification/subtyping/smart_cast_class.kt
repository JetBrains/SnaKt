// ALWAYS_VALIDATE
// Probe: smart casts between class types

open class Shape
open class Rectangle(val width: Int, val height: Int) : Shape()
class Square(val side: Int) : Rectangle(side, side)

// Smart cast from supertype to subtype after is-check
fun <!VIPER_TEXT!>accessAfterSmartCast<!>(s: Shape): Int {
    if (s is Rectangle) {
        return s.width
    }
    return -1
}

// Nested smart cast
fun <!VIPER_TEXT!>nestedSmartCast<!>(s: Shape): Int {
    if (s is Rectangle) {
        if (s is Square) {
            return s.side
        }
        return s.height
    }
    return 0
}
