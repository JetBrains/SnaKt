// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

@Pure
fun <!VIPER_TEXT!>fact<!>(n: Int): Int {
    preconditions {
        n >= 0
    }
    postconditions<Int> { res ->
        (n == 0) implies (res == 1)
        (n > 0) implies (res == n * <!CONSISTENCY!>fact(n-1)<!>)
    }

    return if (n == 0) {
        1
    } else {
        n * fact(n - 1)
    }
}
