// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

class PureFun_Box(private val seed: Int) {
    @Pure
    fun <!VIPER_TEXT!>value <!>(): Int = seed
}

class PureFun {
    class Box(private val seed: Boolean) {
        @Pure
        fun <!VIPER_TEXT!>value <!>(): Boolean = seed
    }
}

fun <!VIPER_TEXT!>pureFlattenedMemberFunctions<!>() {
    val left = PureFun_Box(1)
    val right = PureFun.Box(true)
    left.value()
    right.value()
}
