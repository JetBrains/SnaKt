// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.*

<!ADT_VIOLATION!>@ADT
class NotAnObject<!>

@ADT
<!ADT_VIOLATION!>object NotDataObject<!>

@ADT
data object WithField {
    <!ADT_VIOLATION!>val x: Int = 42<!>
}

@ADT
data object WithFunction {
    <!ADT_VIOLATION!>@NeverConvert
    fun doSomething(): Int = 1<!>
}

@ADT
<!ADT_VIOLATION!>object TriggeredByAssignment<!>

interface SomeInterface

@ADT
data <!ADT_VIOLATION!>object WithSupertype<!> : SomeInterface

@ADT
data object Valid

<!ADT_VIOLATION!>fun triggerNotAnObject(x: NotAnObject) {}<!>
<!ADT_VIOLATION!>fun triggerNotDataObject(x: NotDataObject) {}<!>
<!ADT_VIOLATION!>fun triggerWithField(x: WithField) {}<!>
<!ADT_VIOLATION!>fun triggerWithFunction(x: WithFunction) {}<!>
<!ADT_VIOLATION!>fun triggerWithSupertype(x: WithSupertype) {}<!>
fun triggerWithAssignment() {
    val x = TriggeredByAssignment
}

fun <!VIPER_TEXT!>validParameter<!>(a: Valid) {}

fun <!VIPER_TEXT!>validLocalVariable<!>() {
    val x = Valid
}
