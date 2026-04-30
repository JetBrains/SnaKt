// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.ADT


<!ADT_INVALID_TARGET!>@ADT
class NotAnObject<!>

@ADT
<!ADT_INVALID_TARGET!>object NotDataObject<!>

@ADT
data <!ADT_INVALID_TARGET!>object WithField<!> {
    val x: Int = 42
}

@ADT
data <!ADT_INVALID_TARGET, ADT_INVALID_TARGET!>object WithFunction<!> {
    fun <!VIPER_TEXT!>doSomething<!>(): Int = 1
}

interface SomeInterface

@ADT
data <!ADT_INVALID_TARGET!>object WithSupertype<!> : SomeInterface

@ADT
data object Valid

fun <!VIPER_TEXT!>triggerNotAnObject<!>(x: NotAnObject) {}
fun <!VIPER_TEXT!>triggerNotDataObject<!>(x: NotDataObject) {}
fun <!VIPER_TEXT!>triggerWithField<!>(x: WithField) {}
fun <!VIPER_TEXT!>triggerWithFunction<!>(x: WithFunction) {}
fun <!VIPER_TEXT!>triggerWithSupertype<!>(x: WithSupertype) {}

fun <!VIPER_TEXT!>validParameter<!>(a: Valid) {}

fun <!VIPER_TEXT!>validLocalVariable<!>() {
    val x = Valid
}

fun <!VIPER_TEXT!>equalityNullable<!>(): Boolean {
    val a: Valid? = Valid
    val b: Valid? = Valid
    return a == b
}
