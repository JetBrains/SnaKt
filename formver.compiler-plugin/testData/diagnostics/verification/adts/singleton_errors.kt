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

fun <!VERIFICATION_SKIPPED!>triggerNotAnObject<!>(x: NotAnObject) {}
fun <!VERIFICATION_SKIPPED!>triggerNotDataObject<!>(x: NotDataObject) {}
fun <!VERIFICATION_SKIPPED!>triggerWithField<!>(x: WithField) {}
fun <!VERIFICATION_SKIPPED!>triggerWithFunction<!>(x: WithFunction) {}
fun <!VERIFICATION_SKIPPED!>triggerWithSupertype<!>(x: WithSupertype) {}
fun <!VERIFICATION_SKIPPED!>triggerWithAssignment<!>() {
    val x = <!ADT_VIOLATION!>TriggeredByAssignment<!>
}

fun <!VIPER_TEXT!>validParameter<!>(a: Valid) {}

fun <!VIPER_TEXT!>validLocalVariable<!>() {
    val x = Valid
}
