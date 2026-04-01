// NEVER_VALIDATE

class Setter_Box(seed: Int) {
    var value: Int = seed
        set(v) {
            field = v
        }
}

class Setter {
    class Box(seed: Boolean) {
        var value: Boolean = seed
            set(v) {
                field = v
            }
    }
}

fun <!VIPER_TEXT!>flattenedCustomSetters<!>() {
    val left = Setter_Box(1)
    val right = Setter.Box(true)
    left.value = 2
    right.value = false
}
