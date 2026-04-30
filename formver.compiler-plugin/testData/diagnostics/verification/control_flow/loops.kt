import org.jetbrains.kotlin.formver.plugin.NeverConvert

fun <!VIPER_TEXT!>whileLoop<!>(b: Boolean): Boolean {
    while (b) {
        val a = 1
        val c = 2
    }
    return false
}

fun <!VIPER_TEXT!>whileFunctionCondition<!>() {
    while (whileLoop(true)) {
    }
}

@NeverConvert
fun returnsBoolean(): Boolean {
    return false
}

fun <!VIPER_TEXT!>dynamicLambdaInvariant<!>(f: () -> Int) {
    while (returnsBoolean()) {
        f()
    }
}

fun <!VIPER_TEXT!>functionAssignment<!>(f: () -> Int) {
    val g = f
    while (returnsBoolean()) {
        g()
    }
}

fun <!VIPER_TEXT!>conditionalFunctionAssignment<!>(b: Boolean, f: () -> Int, h: () -> Int) {
    val g = if (b) f else h
    while (returnsBoolean()) {
        g()
    }
}
