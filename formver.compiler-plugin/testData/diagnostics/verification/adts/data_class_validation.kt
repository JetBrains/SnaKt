// NEVER_VALIDATE
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data object Tag

@ADT
data class Box<!VIPER_TEXT!>(val <!VIPER_TEXT!>n<!>: Int)<!>

@ADT
data class TwoFields<!VIPER_TEXT!>(val <!VIPER_TEXT!>a<!>: Int, val <!VIPER_TEXT!>b<!>: Boolean)<!>

@ADT
data class OptInt<!VIPER_TEXT!>(val <!VIPER_TEXT!>n<!>: Int?)<!>

@ADT
data class WithVar(<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>var x: Int<!>)

@ADT
data class WithBodyField(val a: Int) {
    <!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>val b: Int = 42<!>
}

@ADT
data class WithFunction(val a: Int) {
    <!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@NeverConvert
    fun doSomething(): Int = 1<!>
}

<!ADT_VIOLATION, ADT_VIOLATION, ADT_VIOLATION!>@ADT
data class Generic<T>(val x: T)<!>

fun <!VIPER_TEXT!>useBox<!>(b: Box) {}
fun <!VIPER_TEXT!>useTwoFields<!>(p: TwoFields) {}
fun <!VIPER_TEXT!>useOptInt<!>(o: OptInt) {}

<!ADT_VIOLATION!>fun useWithVar(x: WithVar) {}<!>
<!ADT_VIOLATION!>fun useWithBodyField(x: WithBodyField) {}<!>
<!ADT_VIOLATION!>fun useWithFunction(x: WithFunction) {}<!>
<!ADT_VIOLATION!>fun useGeneric(x: Generic<Int>) {}<!>

fun <!VIPER_TEXT!>constrUsage<!>() {
    val i = 2
    val tf = TwoFields(i, true)
}
