import org.jetbrains.kotlin.formver.plugin.*

// Verify that elvis returns the default when input is null
@AlwaysVerify
fun <!VIPER_TEXT!>elvisDefaultOnNull<!>(): Int {
    postconditions<Int> {
        it == 42
    }
    val x: Int? = null
    return x ?: 42
}

// Verify that nullable passthrough preserves identity
@AlwaysVerify
fun <!VIPER_TEXT!>nullablePassthrough<!>(x: Int?): Int? {
    postconditions<Int?> {
        it == x
    }
    return x
}

// Verify returning null literal
@AlwaysVerify
fun <!VIPER_TEXT!>returnNullLiteral<!>(): Int? {
    postconditions<Int?> {
        it == null
    }
    return null
}

// Verify null comparison on a known-null value
@AlwaysVerify
fun <!VIPER_TEXT!>nullComparisonReturnsTrue<!>(): Boolean {
    postconditions<Boolean> {
        it == true
    }
    val x: Int? = null
    return x == null
}

// Verify non-null comparison on a known-null value
@AlwaysVerify
fun <!VIPER_TEXT!>nonNullComparisonReturnsFalse<!>(): Boolean {
    postconditions<Boolean> {
        it == false
    }
    val x: Int? = null
    return x != null
}

// Verify elvis with non-null input passes through
@AlwaysVerify
fun <!VIPER_TEXT!>elvisWithNonNull<!>(x: Int): Int {
    postconditions<Int> {
        it == x
    }
    val y: Int? = x
    return y ?: 0
}

// Verify smart cast after null check in if-else
@AlwaysVerify
fun <!VIPER_TEXT!>smartcastInBranch<!>(n: Int?): Int {
    if (n != null) {
        return n
    }
    return 0
}
