// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.ADT

// --- Invalid: @ADT on non-object class declarations ---

<!ADT_INVALID_TARGET!>@ADT
class AdtOnClass<!>

<!ADT_INVALID_TARGET!>@ADT
open class AdtOnOpenClass<!>

// --- Invalid: @ADT on a non-data object ---

@ADT
<!ADT_INVALID_TARGET!>object AdtNonDataObject<!>

// --- Invalid: @ADT data object with fields ---

@ADT
data <!ADT_INVALID_TARGET!>object AdtWithField<!> {
    val x: Int = 42
}

// --- Invalid: @ADT data object with member functions ---

@ADT
data <!ADT_INVALID_TARGET, ADT_INVALID_TARGET!>object AdtWithFunction<!> {
    fun <!VIPER_TEXT!>doSomething<!>(): Int = 1
}

// --- Invalid: @ADT data object extending a class ---

open class SomeBase

@ADT
data <!ADT_INVALID_TARGET!>object AdtExtendingClass<!> : SomeBase()

// --- Invalid: @ADT data object implementing an interface ---

interface SomeInterface

@ADT
data <!ADT_INVALID_TARGET!>object AdtImplementingInterface<!> : SomeInterface

// --- Valid: bare @ADT data objects ---

@ADT
data object ValidAdt1

@ADT
data object ValidAdt2

// --- Trigger class embedding for invalid ADTs to exercise the checker ---

fun <!VIPER_TEXT!>triggerAdtOnClass<!>(x: AdtOnClass) {}

fun <!VIPER_TEXT!>triggerAdtOnOpenClass<!>(x: AdtOnOpenClass) {}

fun <!VIPER_TEXT!>triggerAdtNonDataObject<!>(x: AdtNonDataObject) {}

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
