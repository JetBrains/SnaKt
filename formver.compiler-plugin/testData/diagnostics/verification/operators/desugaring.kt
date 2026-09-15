// FULL_JDK

class OperatorProbe(var value: Int) {
    operator fun <!VIPER_TEXT!>plus<!>(other: Int): Int = value + other
    operator fun <!VIPER_TEXT!>minus<!>(other: Int): Int = value - other
    operator fun <!VIPER_TEXT!>compareTo<!>(other: Int): Int = value.compareTo(other)
    operator fun <!VIPER_TEXT!>get<!>(index: Int): Int = value + index
    operator fun <!VIPER_TEXT!>set<!>(index: Int, newValue: Int) {
        value = newValue + index
    }
    operator fun <!VIPER_TEXT!>contains<!>(candidate: Int): Boolean = candidate == value
    operator fun <!VIPER_TEXT!>invoke<!>(argument: Int): Int = value + argument
    operator fun <!VIPER_TEXT!>unaryPlus<!>(): Int = value
    operator fun <!VIPER_TEXT!>unaryMinus<!>(): Int = -value
}

fun <!VIPER_TEXT!>plusOperator<!>(probe: OperatorProbe, operand: Int): Int = probe + operand
fun <!VIPER_TEXT!>plusExplicit<!>(probe: OperatorProbe, operand: Int): Int = probe.plus(operand)

fun <!VIPER_TEXT!>minusOperator<!>(probe: OperatorProbe, operand: Int): Int = probe - operand
fun <!VIPER_TEXT!>minusExplicit<!>(probe: OperatorProbe, operand: Int): Int = probe.minus(operand)

fun <!VIPER_TEXT!>lessOperator<!>(probe: OperatorProbe, operand: Int): Boolean = probe < operand
fun <!VIPER_TEXT!>lessExplicit<!>(probe: OperatorProbe, operand: Int): Boolean = probe.compareTo(operand) < 0
fun <!VIPER_TEXT!>greaterEqualOperator<!>(probe: OperatorProbe, operand: Int): Boolean = probe >= operand
fun <!VIPER_TEXT!>greaterEqualExplicit<!>(probe: OperatorProbe, operand: Int): Boolean = probe.compareTo(operand) >= 0

fun <!VIPER_TEXT!>getOperator<!>(probe: OperatorProbe, index: Int): Int = probe[index]
fun <!VIPER_TEXT!>getExplicit<!>(probe: OperatorProbe, index: Int): Int = probe.get(index)

fun <!VIPER_TEXT!>setExplicit<!>(probe: OperatorProbe, index: Int, newValue: Int) {
    probe.set(index, newValue)
}

fun <!VIPER_TEXT!>containsOperator<!>(probe: OperatorProbe, candidate: Int): Boolean = candidate in probe
fun <!VIPER_TEXT!>containsExplicit<!>(probe: OperatorProbe, candidate: Int): Boolean = probe.contains(candidate)
fun <!VIPER_TEXT!>notContainsOperator<!>(probe: OperatorProbe, candidate: Int): Boolean = candidate !in probe
fun <!VIPER_TEXT!>notContainsExplicit<!>(probe: OperatorProbe, candidate: Int): Boolean = !probe.contains(candidate)

fun <!VIPER_TEXT!>invokeOperator<!>(probe: OperatorProbe, argument: Int): Int = probe(argument)
fun <!VIPER_TEXT!>invokeExplicit<!>(probe: OperatorProbe, argument: Int): Int = probe.invoke(argument)

fun <!VIPER_TEXT!>unaryPlusOperator<!>(probe: OperatorProbe): Int = +probe
fun <!VIPER_TEXT!>unaryPlusExplicit<!>(probe: OperatorProbe): Int = probe.unaryPlus()
fun <!VIPER_TEXT!>unaryMinusOperator<!>(probe: OperatorProbe): Int = -probe
fun <!VIPER_TEXT!>unaryMinusExplicit<!>(probe: OperatorProbe): Int = probe.unaryMinus()

fun <!VIPER_TEXT!>primitivePositiveControl<!>(left: Int, right: Int): Boolean =
    left + right == left.plus(right) && left - right == left.minus(right)
