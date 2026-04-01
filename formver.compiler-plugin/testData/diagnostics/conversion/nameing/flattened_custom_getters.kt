// NEVER_VALIDATE

class Getter_Box(val seed: Int) {
    val value: Int
        get() = seed
}

class Getter {
    class Box(val seed: Boolean) {
        val value: Boolean
            get() = seed
    }
}

fun <!VIPER_TEXT!>flattenedCustomGetters<!>() {
    val left = Getter_Box(1)
    val right = Getter.Box(true)
    left.value
    right.value
}
