fun <!VIPER_TEXT!>localFunction<!>() {
    fun local() {}
    local()
}

fun <!VIPER_TEXT!>capturingLocalFunction<!>(x: Int) {
    fun local() {
        val captured = x
    }
    local()
}

// Sibling local functions may share a name and signature; each call must inline its own body.
fun <!VIPER_TEXT!>sameNamedSiblingLocalFunctions<!>() {
    if (true) {
        fun local(): Int = 1
        local()
    }
    if (true) {
        fun local(): Int = 2
        local()
    }
}
