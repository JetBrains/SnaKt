// NEVER_VALIDATE

class Function_Box(val seed: Int) {
    fun <!VIPER_TEXT!>read<!>(): Int = seed
}

class Function {
    class Box(val seed: Boolean) {
        fun <!VIPER_TEXT!>read<!>(): Boolean = seed
    }
}

fun <!VIPER_TEXT!>flattenedMemberFunctions<!>() {
    val left = Function_Box(1)
    val right = Function.Box(true)
    left.read()
    right.read()
}
