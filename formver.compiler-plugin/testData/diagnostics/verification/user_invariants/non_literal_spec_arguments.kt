// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

fun preconditionsFromParameter(body: () -> Unit) {
    <!INTERNAL_ERROR!>preconditions(body)<!>
}

fun loopInvariantsFromParameter(n: Int, body: () -> Unit) {
    var i = 0
    while (i < n) {
        <!INTERNAL_ERROR!>loopInvariants(body)<!>
        i++
    }
}

fun postconditionsWithWrongType(): Int {
    postconditions<Boolean> <!INTERNAL_ERROR!>{ it }<!>
    return 0
}
