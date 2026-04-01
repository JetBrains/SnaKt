// NEVER_VALIDATE

interface Holder_Box {
    val payload: Int
        get() = 1
}

open class Holder_Box_Base : Holder_Box

interface Holder {
    interface Box {
        val payload: Boolean
            get() = true
    }
}

open class Holder_Box_Nested_Base : Holder.Box

fun <!VIPER_TEXT!>flattenedInterfaceBodyProps<!>() {
    val left = Holder_Box_Base()
    val right = Holder_Box_Nested_Base()
    left.payload
    right.payload
}
