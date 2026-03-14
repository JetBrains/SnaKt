// CONVERSION_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique


class PrimitiveFields(
    var shared: Int,
    @Unique var unique: Int
)

fun <!VIPER_TEXT!>testPrimitiveFieldSetterUnique<!>(@Unique pf: PrimitiveFields) {
    pf.shared = 1
    pf.unique = 2
}

fun <!VIPER_TEXT!>testPrimitiveFieldSetterShared<!>(pf: PrimitiveFields) {
    pf.shared = 3
    pf.unique = 4
}


class ReferenceFields(
    var shared: PrimitiveFields,
    @Unique var unique: PrimitiveFields
)

fun <!VIPER_TEXT!>testReferenceFieldSetterUnique<!>(@Unique rf: ReferenceFields) {
    rf.shared = PrimitiveFields(5, 6)
    rf.unique = PrimitiveFields(7, 8)
}

fun <!VIPER_TEXT!>testReferenceFieldSetterShared<!>(rf: ReferenceFields) {
    rf.shared = PrimitiveFields(9, 10)
    rf.unique = PrimitiveFields(11, 12)
}
