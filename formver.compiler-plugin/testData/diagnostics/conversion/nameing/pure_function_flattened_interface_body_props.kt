// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>payload<!>(x: Int): Int = x

interface PureHolder_Box {
    val payload: Int
        get() = 1
}

open class PureHolder_Box_Base : PureHolder_Box

interface PureHolder {
    interface Box {
        val payload: Boolean
            get() = true
    }
}

open class PureHolder_Box_Nested_Base : PureHolder.Box

fun <!VIPER_TEXT!>pureFunctionFlattenedInterfaceBodyProps<!>() {
    val left = PureHolder_Box_Base()
    val right = PureHolder_Box_Nested_Base()
    payload(1)
    left.payload
    right.payload
}
