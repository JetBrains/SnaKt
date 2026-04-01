// NEVER_VALIDATE

interface Carrier_Box {
    val payload: Int
        get() = 1
}

class Carrier_Box_Impl : Carrier_Box

interface Carrier {
    interface Box {
        val payload: Boolean
            get() = true
    }
}

class Carrier_Box_Nested_Impl : Carrier.Box

fun <!VIPER_TEXT!>flattenedInterfaceGetters<!>() {
    val left = Carrier_Box_Impl()
    val right = Carrier_Box_Nested_Impl()
    left.payload
    right.payload
}
