// NEVER_VALIDATE

class OverloadedCtor_Box {
    val first: Int

    constructor(x: Int) {
        first = x
    }

    constructor(x: Boolean) {
        first = if (x) 1 else 0
    }
}

class OverloadedCtor {
    class Box {
        val second: Boolean

        constructor(x: Int) {
            second = x > 0
        }

        constructor(x: Boolean) {
            second = x
        }
    }
}

fun <!VIPER_TEXT!>flattenedSecondaryConstructorOverloads<!>() {
    val a = OverloadedCtor_Box(1)
    val b = OverloadedCtor_Box(false)
    val c = OverloadedCtor.Box(2)
    val d = OverloadedCtor.Box(true)
    a.first
    b.first
    c.second
    d.second
}
