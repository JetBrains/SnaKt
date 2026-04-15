// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.ADT

// --- Invalid: @ADT on non-object class declarations ---

<!ADT_INVALID_TARGET!>@ADT
class AdtOnClass<!>

<!ADT_INVALID_TARGET!>@ADT
open class AdtOnOpenClass<!>

// --- Invalid: @ADT object with fields ---

@ADT
<!ADT_INVALID_TARGET!>object AdtWithField<!> {
    val x: Int = 42
}

// --- Invalid: @ADT object with member functions ---

@ADT
<!ADT_INVALID_TARGET, ADT_INVALID_TARGET!>object AdtWithFunction<!> {
    fun <!VIPER_TEXT!>doSomething<!>(): Int = 1
}

// --- Invalid: @ADT object extending a class ---

open class SomeBase

@ADT
<!ADT_INVALID_TARGET!>object AdtExtendingClass<!> : SomeBase()

// --- Invalid: @ADT object implementing an interface ---

interface SomeInterface

@ADT
<!ADT_INVALID_TARGET!>object AdtImplementingInterface<!> : SomeInterface

// --- Valid: bare @ADT objects ---

@ADT
object ValidAdt1

@ADT
object ValidAdt2

// --- Trigger class embedding for invalid ADTs to exercise the checker ---

fun <!VIPER_TEXT!>triggerAdtOnClass<!>(x: AdtOnClass) {}

fun <!VIPER_TEXT!>triggerAdtOnOpenClass<!>(x: AdtOnOpenClass) {}

fun <!VIPER_TEXT!>triggerAdtWithField<!>(x: AdtWithField) {}

fun <!VIPER_TEXT!>triggerAdtWithFunction<!>(x: AdtWithFunction) {}

fun <!VIPER_TEXT!>triggerAdtExtendingClass<!>(x: AdtExtendingClass) {}

fun <!VIPER_TEXT!>triggerAdtImplementingInterface<!>(x: AdtImplementingInterface) {}

// --- Functions using valid ADTs: single parameter ---

fun <!VIPER_TEXT!>adtParameter<!>(a: ValidAdt1) {}

// --- Functions using valid ADTs: two ADT parameters ---

fun <!VIPER_TEXT!>twoAdtParameters<!>(a: ValidAdt1, b: ValidAdt2) {}

// --- Functions using valid ADTs: mixed ADT + non-ADT parameter ---

fun <!VIPER_TEXT!>mixedParameters<!>(a: ValidAdt1, n: Int) {}

// --- Functions using valid ADTs: local variable ---

fun <!VIPER_TEXT!>adtLocalVariable<!>() {
    val x = ValidAdt1
}