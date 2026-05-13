// NEVER_VALIDATE
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@ADT
data class TwoFields(val a: Int, val b: Boolean)

@ADT
data class OptInt(val n: Int?)

@ADT
data class Node(val head: Int, val tail: Node?)

@ADT
data class WithVar(<!ADT_VIOLATION!>var x: Int<!>)

@ADT
data class WithBodyField(val a: Int) {
    <!ADT_VIOLATION!>val b: Int = 42<!>
}

@ADT
data class WithFunction(val a: Int) {
    <!ADT_VIOLATION!>@NeverConvert
    fun doSomething(): Int = 1<!>
}

<!ADT_VIOLATION!>@ADT
data class Generic<T>(val x: T)<!>

fun <!VIPER_TEXT!>useTwoFields<!>(p: TwoFields) {}
fun <!VIPER_TEXT!>useOptInt<!>(o: OptInt) {}
fun <!VIPER_TEXT!>useNode<!>(n: Node) {}

<!ADT_VIOLATION!>fun useWithVar(x: WithVar) {}<!>
<!ADT_VIOLATION!>fun useWithBodyField(x: WithBodyField) {}<!>
<!ADT_VIOLATION!>fun useWithFunction(x: WithFunction) {}<!>
<!ADT_VIOLATION!>fun useGeneric(x: Generic<Int>) {}<!>

fun <!VIPER_TEXT!>testConstrUsage<!>() {
    val i = 2
    val tf = TwoFields(i, true)
}

fun <!VIPER_TEXT!>testDestrUsage<!>() {
    val tf = TwoFields(2, true)
    val i = tf.a
    val b = tf.b
}
