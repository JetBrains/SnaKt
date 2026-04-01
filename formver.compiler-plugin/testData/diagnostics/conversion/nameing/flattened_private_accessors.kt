// NEVER_VALIDATE

class PrivateAccessor_Box(seed: Int) {
    private var value: Int = seed
        get() = field
        set(v) {
            field = v
        }

    fun <!VIPER_TEXT!>touch<!>()
    {
        value = value + 1
    }
}

class PrivateAccessor {
    class Box(seed: Boolean) {
        private var value: Boolean = seed
            get() = field
            set(v) {
                field = v
            }

        fun <!VIPER_TEXT!>touch<!>()
        {
            value = !value
        }
    }
}

fun <!VIPER_TEXT!>flattenedPrivateAccessors<!>() {
    val left = PrivateAccessor_Box(1)
    val right = PrivateAccessor.Box(true)
    left.touch()
    right.touch()
}
