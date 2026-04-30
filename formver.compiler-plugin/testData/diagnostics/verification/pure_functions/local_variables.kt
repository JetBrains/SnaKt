// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>returnNumberVal<!>(): Int {
    val x = 42
    return x
}

@Pure
fun <!VIPER_TEXT!>multipleAssignmentsOfDifferentType<!>(): Boolean {
    val a = 42
    val b = "Hello SnaKt"
    val c = true
    val d = 'A'
    return c
}

@Pure
fun <!VIPER_TEXT!>multipleAssignmentsWithLiteralReturn<!>(): Int {
    val a = 42
    val b = "Hello SnaKt"
    val c = true
    val d = 'A'
    return 42
}

@Pure
fun <!VIPER_TEXT!>laterInitializersCanRelyOnPrevious<!>(): Int {
    val a = 40
    val b = a + 2
    val c = b * 2
    return c
}

@Pure
fun <!VIPER_TEXT!>initializersCanRelyOnParameters<!>(x: Int, y: Int): Int {
    val sum = x + y
    val diff = x - y
    val res = sum * diff
    return res
}

@Pure
fun <!VIPER_TEXT!>doubleIncrement<!>(): Int {
    var x = 1
    x = x + 1
    x = x + 1
    return x
}

@Pure
fun <!VIPER_TEXT!>updateThenReadIntoOther<!>(): Int {
    var x = 3
    x = x + 4
    val y = x + 1
    return y
}

@Pure
fun <!VIPER_TEXT!>readOldValueBeforeUpdate<!>(): Int {
    var x = 10
    val y = x + 1
    x = x + 5
    x = y + x
    return x
}

@Pure
fun <!VIPER_TEXT!>chainThroughTemp<!>(): Int {
    var x = 2
    val t = x + 3
    x = t + 4
    return x
}

@Pure
fun <!VIPER_TEXT!>overwriteNotSelfReferential<!>(): Int {
    var x = 7
    x = 100
    return x
}

@Pure
fun <!VIPER_TEXT!>nestedConditionalAssignment<!>(a: Boolean, b: Boolean): Int {
    var x = if (a) (if (b) 4 else 3) else (if (b) 2 else 1)
    return x
}

@Pure
fun <!VIPER_TEXT!>blockConditionalAssignment<!>(a: Boolean, b: Boolean): Int {
    var x = if (a) {
        var y = if (b) 10 else 20
        y + 1
    } else {
        30
    }
    return x
}

@Pure
fun <!VIPER_TEXT!>whenAssignment<!>(a: Int, b: Boolean): Int {
    var x = when (a) {
        1 -> if (b) 2 else 3
        2 -> 4
        else -> 5
    }
    return x
}
