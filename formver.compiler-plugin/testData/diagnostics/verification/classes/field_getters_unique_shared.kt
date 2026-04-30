import org.jetbrains.kotlin.formver.plugin.Unique


class PrimitiveFields(
    val sharedVal: Int,
    var sharedVar: Int,
    @Unique val uniqueVal: Int,
    @Unique var uniqueVar: Int
)

fun <!VIPER_TEXT!>testPrimitiveFieldGetterUnique<!>(@Unique pf: PrimitiveFields) {
    val sharedVal = pf.sharedVal
    var sharedVar = pf.sharedVar
    val uniqueVal = pf.uniqueVal
    var uniqueVar = pf.uniqueVar
}

fun <!VIPER_TEXT!>testPrimitiveFieldGetterShared<!>(pf: PrimitiveFields) {
    val sharedVal = pf.sharedVal
    var sharedVar = pf.sharedVar
    val uniqueVal = pf.uniqueVal
    var uniqueVar = pf.uniqueVar
}


class ReferenceFields(
    val sharedVal: PrimitiveFields,
    var sharedVar: PrimitiveFields,
    @Unique val uniqueVal: PrimitiveFields,
    @Unique var uniqueVar: PrimitiveFields
)

fun <!VIPER_TEXT!>testReferenceFieldGetterUnique<!>(@Unique rf: ReferenceFields) {
    val sharedVal = rf.sharedVal
    var sharedVar = rf.sharedVar
    val uniqueVal = rf.uniqueVal
    var uniqueVar = rf.uniqueVar
}

fun <!VIPER_TEXT!>testReferenceFieldGetterShared<!>(rf: ReferenceFields) {
    val sharedVal = rf.sharedVal
    var sharedVar = rf.sharedVar
    val uniqueVal = rf.uniqueVal
    var uniqueVar = rf.uniqueVar
}
