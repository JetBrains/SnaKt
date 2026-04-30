import org.jetbrains.kotlin.formver.plugin.Unique

class UniquePrimitiveFields(
    val sharedVal: Int,
    var sharedVar: Int,
    @Unique val uniqueVal: Int,
    @Unique var uniqueVar: Int
)

fun <!VIPER_TEXT!>testPrimitiveFieldGetterUnique<!>(@Unique pf: UniquePrimitiveFields) {
    val sharedVal = pf.sharedVal
    var sharedVar = pf.sharedVar
    val uniqueVal = pf.uniqueVal
    var uniqueVar = pf.uniqueVar
}

fun <!VIPER_TEXT!>testPrimitiveFieldGetterShared<!>(pf: UniquePrimitiveFields) {
    val sharedVal = pf.sharedVal
    var sharedVar = pf.sharedVar
    val uniqueVal = pf.uniqueVal
    var uniqueVar = pf.uniqueVar
}

fun <!VIPER_TEXT!>testPrimitiveFieldSetterUnique<!>(@Unique pf: UniquePrimitiveFields) {
    pf.sharedVar = 1
    pf.uniqueVar = 2
}

fun <!VIPER_TEXT!>testPrimitiveFieldSetterShared<!>(pf: UniquePrimitiveFields) {
    pf.sharedVar = 3
    pf.uniqueVar = 4
}

class UniqueReferenceFields(
    val sharedVal: UniquePrimitiveFields,
    var sharedVar: UniquePrimitiveFields,
    @Unique val uniqueVal: UniquePrimitiveFields,
    @Unique var uniqueVar: UniquePrimitiveFields
)

fun <!VIPER_TEXT!>testReferenceFieldGetterUnique<!>(@Unique rf: UniqueReferenceFields) {
    val sharedVal = rf.sharedVal
    var sharedVar = rf.sharedVar
    val uniqueVal = rf.uniqueVal
    var uniqueVar = rf.uniqueVar
}

fun <!VIPER_TEXT!>testReferenceFieldGetterShared<!>(rf: UniqueReferenceFields) {
    val sharedVal = rf.sharedVal
    var sharedVar = rf.sharedVar
    val uniqueVal = rf.uniqueVal
    var uniqueVar = rf.uniqueVar
}

fun <!VIPER_TEXT!>testReferenceFieldSetterUnique<!>(@Unique rf: UniqueReferenceFields) {
    rf.sharedVar = UniquePrimitiveFields(5, 6, 7, 8)
    rf.uniqueVar = UniquePrimitiveFields(9, 10, 11, 12)
}

fun <!VIPER_TEXT!>testReferenceFieldSetterShared<!>(rf: UniqueReferenceFields) {
    rf.sharedVar = UniquePrimitiveFields(13, 14, 15, 16)
    rf.uniqueVar = UniquePrimitiveFields(17, 18, 19, 20)
}
