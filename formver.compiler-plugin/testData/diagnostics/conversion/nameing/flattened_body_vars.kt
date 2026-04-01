// NEVER_VALIDATE

class BodyVar_Box(seed: Int) {
    var value = seed
}

class BodyVar {
    class Box(seed: Boolean) {
        var value = seed
    }
}

fun <!VIPER_TEXT!>flattenedBodyVars<!>() {
    val left = BodyVar_Box(1)
    val right = BodyVar.Box(true)
    left.value = 2
    right.value = false
}
