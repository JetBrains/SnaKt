// RENDER_PREDICATES
// WITH_UNIQUENESS
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify
import org.jetbrains.kotlin.formver.plugin.NeverConvert

class Node(var data: Int, val left: @Unique Node?, val right: @Unique Node?)

fun <!VIPER_TEXT!>get_left_val<!>(n: @Unique Node): Int? {
    return <!VIPER_VERIFICATION_ERROR!>n.left?.data<!>
}

// these expressions should all verify - they currently do not due to lack of correct fold+unfolding
@AlwaysVerify
fun <!VIPER_TEXT!>test<!>() {
    val n: @Unique Node = Node(5, Node(4, null, null), Node(3, Node(2, null, null), Node(1, null, null)))
    val expr1 = n.data == 5
    verify(expr1)
    val expr2 = n.left?.data == 4
    verify(<!VIPER_VERIFICATION_ERROR!>expr2<!>)
}
