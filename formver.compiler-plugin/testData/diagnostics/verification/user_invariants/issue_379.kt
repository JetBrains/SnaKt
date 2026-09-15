// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun <!VIPER_TEXT!>requiresNonEmptyList<!>(list: List<Int>) {
    preconditions { list.size > 0 }
}
