// NEVER_VALIDATE

class BodyVal_Box(seed: Int) {
    val value = seed
}

class BodyVal {
    class Box(seed: Boolean) {
        val value = seed
    }
}

fun <!VIPER_TEXT!>flattenedBodyVals<!>() {
    val left = BodyVal_Box(1)
    val right = BodyVal.Box(true)
    left.value
    right.value
}
