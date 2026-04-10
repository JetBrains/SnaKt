// ALWAYS_VALIDATE
// Probe: method override verification

open class Counter(val start: Int) {
    open fun <!VIPER_TEXT!>count<!>(): Int = start
}

class DoubleCounter(start: Int) : Counter(start) {
    override fun <!VIPER_TEXT!>count<!>(): Int = start * 2
}

// Call overridden method on subtype
fun <!VIPER_TEXT!>callDoubleCount<!>(dc: DoubleCounter): Int {
    return dc.count()
}

// Call inherited method via supertype parameter
fun <!VIPER_TEXT!>callOnSupertype<!>(c: Counter): Int {
    return c.count()
}

// Method override dispatch: what does the verifier think happens
// when a DoubleCounter is passed where Counter is expected?
fun <!VIPER_TEXT!>subtypeMethodCall<!>(dc: DoubleCounter): Int {
    val c: Counter = dc
    return c.count()
}
