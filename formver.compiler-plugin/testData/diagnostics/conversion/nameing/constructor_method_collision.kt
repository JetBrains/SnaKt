// NEVER_VALIDATE

fun <!VIPER_TEXT!>con<!>(x: Boolean) {
}

class ConstructorCollision {
    class Box(val x: Boolean)
}

fun <!VIPER_TEXT!>constructorMethodCollision<!>() {
    con(true)
    val box = ConstructorCollision.Box(false)
    box.x
}
