// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

class OperatorProbe(val value: Int) {
    operator fun <!VIPER_TEXT!>invoke<!>(argument: Int): Int = value + argument
}

fun <!VIPER_TEXT!>operatorSyntax<!>(probe: OperatorProbe, argument: Int): Int = probe(argument)

fun <!VIPER_TEXT!>explicitSyntax<!>(probe: OperatorProbe, argument: Int): Int = probe.invoke(argument)

fun <!VIPER_TEXT!>functionTypeSyntax<!>(operation: (Int) -> Int, argument: Int): Int = operation(argument)

fun <!VIPER_TEXT!>explicitFunctionTypeSyntax<!>(operation: (Int) -> Int, argument: Int): Int = operation.invoke(argument)

class SpecifiedProbe(val value: Int) {
    operator fun <!VIPER_TEXT!>invoke<!>(argument: Int): Int {
        postconditions<Int> { res -> res == value + argument }
        return value + argument
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>reliesOnInvokePostcondition<!>(probe: SpecifiedProbe, argument: Int): Int {
    postconditions<Int> { res -> res == probe.value + argument }
    return probe(argument)
}

fun <!VIPER_TEXT!>makeProbe<!>(value: Int): OperatorProbe = OperatorProbe(value)

fun <!VIPER_TEXT!>callResultReceiver<!>(argument: Int): Int = makeProbe(argument)(argument)

fun <!VIPER_TEXT!>functionTypeCallResultReceiver<!>(make: () -> (Int) -> Int, argument: Int): Int = make()(argument)

@AlwaysVerify
fun <!VIPER_TEXT!>lambdaReceiver<!>(argument: Int): Int {
    postconditions<Int> { res -> res == argument + 1 }
    return { x: Int -> x + 1 }(argument)
}

@NeverConvert
inline fun explicitInvokeInline(f: (Int) -> Int): Int = f.invoke(0)

@AlwaysVerify
fun <!VIPER_TEXT!>inlinedExplicitInvoke<!>(): Int {
    postconditions<Int> { res -> res == 1 }
    return explicitInvokeInline { it + 1 }
}
