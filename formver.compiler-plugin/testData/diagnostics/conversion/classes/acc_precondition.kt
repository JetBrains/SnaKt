// RENDER_PREDICATES
// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Manual

class X(@property:Manual var a: Any)
fun <!VIPER_TEXT!>test_acc_precondition<!>(var x: X) {
    preconditions { acc(x.a, write) }
    x.a = 123
}
