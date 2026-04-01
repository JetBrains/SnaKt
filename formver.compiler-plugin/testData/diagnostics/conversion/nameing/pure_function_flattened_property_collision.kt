// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>value <!>(x: Int): Int = x

class Value_Box(val seed: Int) {
    val value: Int
        get() = seed
}

class Value {
    class Box(val seed: Boolean) {
        val value: Boolean
            get() = seed
    }
}

fun <!VIPER_TEXT!>pureFunctionFlattenedPropertyCollision<!>() {
    val left = Value_Box(1)
    val right = Value.Box(true)
    value(0)
    left.value
    right.value
}
