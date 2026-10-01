// FULL_JDK

open class DispatchBase {
    open fun <!VIPER_TEXT!>value<!>(): Int = 1
}

class DispatchDerived : DispatchBase() {
    override fun <!VIPER_TEXT!>value<!>(): Int = 2
}

class DispatchInherited : DispatchBase()

fun <!VIPER_TEXT!>callThroughDerivedReceiver<!>(receiver: DispatchDerived): Int =
    receiver.value()

fun <!VIPER_TEXT!>callThroughBaseReceiver<!>(receiver: DispatchBase): Int =
    receiver.value()

fun <!VIPER_TEXT!>callNewDerivedThroughDerivedReceiver<!>(): Int {
    val receiver = DispatchDerived()
    return receiver.value()
}

fun <!VIPER_TEXT!>callNewDerivedThroughBaseReceiver<!>(): Int {
    val receiver: DispatchBase = DispatchDerived()
    return receiver.value()
}

fun <!VIPER_TEXT!>callInheritedMethod<!>(receiver: DispatchInherited): Int =
    receiver.value()
