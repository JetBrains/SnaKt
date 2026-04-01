// NEVER_VALIDATE

class VerifyFun_Box(private val seed: Int) {
    fun <!VIPER_TEXT!>verify<!>(): Int = seed
}

class VerifyFun {
    class Box(private val seed: Boolean) {
        fun <!VIPER_TEXT!>verify<!>(): Boolean = seed
    }
}

fun <!VIPER_TEXT!>specialVerifyMemberFunctions<!>() {
    val left = VerifyFun_Box(1)
    val right = VerifyFun.Box(true)
    left.verify()
    right.verify()
}
