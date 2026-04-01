// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>verify<!>(x: Int): Int = x + 1

class Verify_Box(val seed: Int) {
    val verify: Int
        get() = seed
}

class Verify {
    class Box(val seed: Boolean) {
        val verify: Boolean
            get() = seed
    }
}

fun <!VIPER_TEXT!>pureFunctionFlattenedVerifyPropertyCollision<!>() {
    val left = Verify_Box(1)
    val right = Verify.Box(true)
    verify(0)
    left.verify
    right.verify
}
