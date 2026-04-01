// NEVER_VALIDATE

fun <!VIPER_TEXT!>con<!>(x: Int) {
}

class ConstructorCollisionInt(val x: Int)

fun <!VIPER_TEXT!>constructorMethodCollisionInt<!>() {
    con(1)
    val box = ConstructorCollisionInt(2)
    box.x
}
