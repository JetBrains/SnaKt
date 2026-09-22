class PrimitiveProperty {
    var aProp: Int = 0
        set(v) {}
}

// The non-null assertion cannot be discharged for a nullable parameter,
// so verification of the generated `assert receiver != null` must fail.
fun <!VIPER_TEXT!>nonNullAssertionReceiverMightFail<!>(property: PrimitiveProperty?) {
    <!VIPER_VERIFICATION_ERROR!>property!!<!>.aProp = 1
}
