// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>payload<!>(x: Int): Int = x + 1

class Payload_Box(val seed: Int) {
    val payload: Int
        get() = seed
}

fun <!VIPER_TEXT!>pureFunctionPropertyCollision<!>() {
    val box = Payload_Box(1)
    val a = payload(2)
    val b = box.payload
}
