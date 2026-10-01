// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>recursiveIdentityPositive<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result == n }

    return if (n == 0) 0 else 1 + recursiveIdentityPositive(n - 1)
}

// The base-case mutation from 0 to 1 contradicts the unchanged identity contract.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>recursiveIdentityMutatedBase<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result == n }

    return if (n == 0) 1 else 1 + recursiveIdentityMutatedBase(n - 1)
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>incrementWithContract<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 1 }
    return value + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedCallsPositive<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 2 }
    return incrementWithContract(incrementWithContract(value))
}

// The composed calls still add two, so strengthening the result by one must fail.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>nestedCallsMutatedContract<!>(value: Int): Int {
    postconditions<Int> { result -> result == value + 3 }
    return incrementWithContract(incrementWithContract(value))
}<!>
