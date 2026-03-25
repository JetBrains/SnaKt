// RENDER_PREDICATES
// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.*

class X(@property:Manual var a: Any)
fun <!VIPER_TEXT!>test_acc_precondition<!>(x: X) {
    preconditions { acc(x.a, write) }
    x.a = 123
}
