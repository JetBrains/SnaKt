// FULL_JDK

package diagnostics.verification.adts

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class Point(val x: Int, val y: Int)

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>translate<!>(p: Point, dx: Int, dy: Int): Point = Point(p.x + dx, p.y + dy)

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>abs<!>(n: Int): Int {
    postconditions<Int> { result ->
        result >= 0
    }
    return if (n >= 0) n else -n
}

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>manhattan<!>(a: Point, b: Point): Int = abs(a.x - b.x) + abs(a.y - b.y)

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>dot<!>(a: Point, b: Point): Int = a.x * b.x + a.y * b.y

@Pure
@AlwaysVerify
fun <!VIPER_TEXT!>normSq<!>(p: Point): Int {
    postconditions<Int> { result ->
        result >= 0
    }
    return p.x * p.x + p.y * p.y
}

@AlwaysVerify
fun <!VIPER_TEXT!>translateZeroIsId<!>(p: Point) {
    postconditions<Unit> { _ ->
        translate(p, 0, 0) == p
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>manhattanSymmetric<!>(a: Point, b: Point) {
    postconditions<Unit> { _ ->
        manhattan(a, b) == manhattan(b, a)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>dotCommutes<!>(a: Point, b: Point) {
    postconditions<Unit> { _ ->
        dot(a, b) == dot(b, a)
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>nonNegativeSquare<!>(u: Int) {
    postconditions<Unit> { _ ->
        u * u >= 0
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>cauchySchwarz<!>(a: Point, b: Point) {
    postconditions<Unit> { _ ->
        dot(a, b) * dot(a, b) <= normSq(a) * normSq(b)
    }
    nonNegativeSquare(a.x * b.y - a.y * b.x)
}

@Pure
fun <!VIPER_TEXT!>rot90<!>(p: Point): Point = Point(-p.y, p.x)

@AlwaysVerify
fun <!VIPER_TEXT!>rot90FourTimesIsId<!>(p: Point) {
    postconditions<Unit> { _ ->
        rot90(rot90(rot90(rot90(p)))) == p
    }
}
