// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>decreasingCountdown<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result == 0 }

    return if (n == 0) 0 else decreasingCountdown(n - 1)
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>incorrectDecreasingIdentity<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result == n }

    return if (n == 0) 0 else incorrectDecreasingIdentity(n - 1)
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>stationaryRecursion<!>(): Int {
    postconditions<Int> { result -> result == 0 }
    return stationaryRecursion()
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutualEvenStep<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result == 0 }

    return if (n == 0) 0 else mutualOddStep(n - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>mutualOddStep<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { result -> result == 0 }

    return if (n == 0) 0 else mutualEvenStep(n - 1)
}

@Pure
fun <!VIPER_TEXT!>pureCountdown<!>(n: Int): Int {
    preconditions { n >= 0 }
    return if (n == 0) 0 else pureCountdown(n - 1)
}

<!VIPER_VERIFICATION_ERROR!>@Pure
fun <!VIPER_TEXT!>pureMutualEven<!>(n: Int): Boolean {
    preconditions { n >= 0 }
    return if (n == 0) true else pureMutualOdd(n - 1)
}<!>

<!VIPER_VERIFICATION_ERROR!>@Pure
fun <!VIPER_TEXT!>pureMutualOdd<!>(n: Int): Boolean {
    preconditions { n >= 0 }
    return if (n == 0) false else pureMutualEven(n - 1)
}<!>
