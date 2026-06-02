// WITH_UNIQUENESS

import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var field: @Unique Int = <!UNIQUENESS_MISMATCH!>1<!>
}

fun <!VIPER_TEXT!>test<!>(a: @Unique A, aShared: A) {
    a.field = <!UNIQUENESS_MISMATCH!>42<!>
    aShared.field = 43
}

fun <!VIPER_TEXT!>test2<!>(a: @Unique A, aShared: A) {
    val a = a.field
    val y = aShared.field
}
