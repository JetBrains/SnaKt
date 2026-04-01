// NEVER_VALIDATE

class Constructor_Box {
    val value: Int

    constructor(x: Int) {
        value = x
    }
}

class Constructor {
    class Box {
        val value: Boolean

        constructor(x: Boolean) {
            value = x
        }
    }
}

fun <!VIPER_TEXT!>flattenedSecondaryConstructors<!>() {
    val left = Constructor_Box(1)
    val right = Constructor.Box(true)
    left.value
    right.value
}
