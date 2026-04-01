// NEVER_VALIDATE

class Shadow_Box(val payload: Int) {
    private val value: Int = payload

    fun <!VIPER_TEXT!>reveal<!>() = value
}

class Shadow {
    class Box(val payload: Boolean) {
        private val value: Boolean = payload

        fun <!VIPER_TEXT!>reveal<!>() = value
    }
}

fun <!VIPER_TEXT!>flattenedPrivateMembers<!>() {
    val left = Shadow_Box(0)
    val right = Shadow.Box(true)
    left.payload
    right.payload
    left.reveal()
    right.reveal()
}
