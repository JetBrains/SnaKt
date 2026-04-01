// NEVER_VALIDATE

class Alpha_Beta(val value: Int)

class Alpha {
    class Beta(val value: Boolean)
}

fun <!VIPER_TEXT!>flattenedClassNames<!>() {
    val left = Alpha_Beta(0)
    val right = Alpha.Beta(true)
    left.value
    right.value
}
