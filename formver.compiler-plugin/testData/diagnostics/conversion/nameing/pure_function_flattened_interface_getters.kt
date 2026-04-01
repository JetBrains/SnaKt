// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>payload<!>(x: Int): Int = x + 1

interface PureCarrier_Box {
    val payload: Int
        get() = 1
}

class PureCarrier_Box_Impl : PureCarrier_Box

interface PureCarrier {
    interface Box {
        val payload: Boolean
            get() = true
    }
}

class PureCarrier_Box_Nested_Impl : PureCarrier.Box

fun <!VIPER_TEXT!>pureFunctionFlattenedInterfaceGetters<!>() {
    val left = PureCarrier_Box_Impl()
    val right = PureCarrier_Box_Nested_Impl()
    payload(0)
    left.payload
    right.payload
}
