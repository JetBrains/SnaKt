// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>preconditions<!>(x: Int): Int = x

@Pure
fun <!VIPER_TEXT!>postconditions<!>(x: Int): Int = x + 1

class Spec_Box(val seed: Int) {
    val preconditions: Int
        get() = seed
}

class Spec {
    class Box(val seed: Boolean) {
        val postconditions: Boolean
            get() = seed
    }
}

fun <!VIPER_TEXT!>pureFunctionFlattenedSpecNames<!>() {
    val left = Spec_Box(1)
    val right = Spec.Box(true)
    preconditions(0)
    postconditions(1)
    left.preconditions
    right.postconditions
}
