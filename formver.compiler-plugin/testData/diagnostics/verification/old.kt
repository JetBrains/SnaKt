// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*


class C(
    @Unique var field: Int
)

fun <!VIPER_TEXT!>test<!>(@Unique @Borrowed c: C) {
    preconditions {
        c.field == 42
    }
    postconditions<Unit> {
        c.field == 43
    }
    inc(c)
    verify(c == old(c))
}

// TODO: Remove the @NeverConvert once we have uniqueness information.
@NeverConvert
fun inc(@Unique @Borrowed c: C) {
    postconditions<Unit> {
        c.field == old(c.field) + 1
    }
    c.field = c.field + 1
}
