// NEVER_VALIDATE

class PrimitiveProperty {
    val nProp: Int
        get() {
            return 0
        }

    var aProp: Int = 0
        set(v) {}
}

fun <!VIPER_TEXT!>testPrimitivePropertyGetter<!>(pp: PrimitiveProperty) : Int = pp.nProp

fun <!VIPER_TEXT!>testPrimitivePropertySetter<!>(pp: PrimitiveProperty) {
    pp.aProp = 0
}

class ReferenceProperty {
    val rProp: PrimitiveProperty
        get() {
            return PrimitiveProperty()
        }

    var ppProp: PrimitiveProperty = PrimitiveProperty()
        set(v) {}
}

fun <!VIPER_TEXT!>testReferencePropertyGetter<!>(rp: ReferenceProperty) {
    val pp = rp.rProp
    val ppn = pp.nProp
}

fun <!VIPER_TEXT!>testCascadingPropertyGetter<!>(rp: ReferenceProperty) {
    val ppn = rp.rProp.nProp
}

fun <!VIPER_TEXT!>testReferencePropertySetter<!>(rp: ReferenceProperty) {
    rp.ppProp = PrimitiveProperty()
}
