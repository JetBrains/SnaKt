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
