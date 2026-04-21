import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A(
    @Unique val next: A?
)

fun <!VIPER_TEXT!>consume<!>(@Unique a : A) {}

fun <!VIPER_TEXT!>test<!>(@Unique a: A) {
    val current = a.next
    if (current != null) {
        val local = current.next
        consume(current)

    }
}
