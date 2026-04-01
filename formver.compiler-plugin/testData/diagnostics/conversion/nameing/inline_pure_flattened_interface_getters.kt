// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>payload<!>(x: Int): Int = x + 1

@NeverConvert
inline fun useInt(block: () -> Int): Int = block()

interface InlineCarrier_Box {
    val payload: Int
        get() = 1
}

class InlineCarrier_Box_Impl : InlineCarrier_Box

interface InlineCarrier {
    interface Box {
        val payload: Boolean
            get() = true
    }
}

class InlineCarrier_Box_Nested_Impl : InlineCarrier.Box

fun <!VIPER_TEXT!>inlinePureFlattenedInterfaceGetters<!>() {
    val left = InlineCarrier_Box_Impl()
    val right = InlineCarrier_Box_Nested_Impl()
    useInt { payload(0) }
    left.payload
    right.payload
}
