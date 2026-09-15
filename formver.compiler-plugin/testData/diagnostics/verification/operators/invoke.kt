// FULL_JDK

class OperatorProbe(val value: Int) {
    operator fun <!VIPER_TEXT!>invoke<!>(argument: Int): Int = value + argument
}

fun <!VIPER_TEXT!>operatorSyntax<!>(probe: OperatorProbe, argument: Int): Int = probe(argument)

fun <!VIPER_TEXT!>explicitSyntax<!>(probe: OperatorProbe, argument: Int): Int = probe.invoke(argument)

fun <!VIPER_TEXT!>functionTypeSyntax<!>(operation: (Int) -> Int, argument: Int): Int = operation(argument)
