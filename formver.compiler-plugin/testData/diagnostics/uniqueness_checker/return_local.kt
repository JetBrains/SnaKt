// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun consume(a: @Unique Any) {}

fun share(a: Any) {}

fun `return shared`(a: Any): Any {
    return a
}

fun `return borrowed`(a: @Borrowed Any): Any {
    return <!LOCALITY_MISMATCH!>a<!>
}

fun `return unique`(a: @Unique Any): Any {
    return a
}

fun `return unique-borrowed`(a: @Unique @Borrowed Any): Any {
    return <!LOCALITY_MISMATCH!>a<!>
}
