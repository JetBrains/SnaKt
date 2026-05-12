// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_INVALID_TARGET!>@ADT
class NotAnObject<!>

@ADT
<!ADT_INVALID_TARGET!>object NotDataObject<!>

@ADT
data <!ADT_INVALID_TARGET!>object WithField<!> {
    val x: Int = 42
}

@ADT
data <!ADT_INVALID_TARGET!>object WithFunction<!> {
    @NeverConvert
    fun doSomething(): Int = 1
}

@ADT
<!ADT_INVALID_TARGET!>object TriggeredByAssignment<!>

interface SomeInterface

@ADT
data <!ADT_INVALID_TARGET!>object WithSupertype<!> : SomeInterface

@ADT
data object Valid

<!ADT_INVALID_USAGE!>fun triggerNotAnObject(x: NotAnObject) {}<!>
<!ADT_INVALID_USAGE!>fun triggerNotDataObject(x: NotDataObject) {}<!>
<!ADT_INVALID_USAGE!>fun triggerWithField(x: WithField) {}<!>
<!ADT_INVALID_USAGE!>fun triggerWithFunction(x: WithFunction) {}<!>
<!ADT_INVALID_USAGE!>fun triggerWithSupertype(x: WithSupertype) {}<!>
<!ADT_INVALID_USAGE!>fun triggerWithAssignment() {
    val x = TriggeredByAssignment
}<!>

fun <!VIPER_TEXT!>validParameter<!>(a: Valid) {}

fun <!VIPER_TEXT!>validLocalVariable<!>() {
    val x = Valid
}
