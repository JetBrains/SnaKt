// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.postconditions

@AlwaysVerify
fun <!VIPER_TEXT!>branchEarlyReturns<!>(first: Boolean): Int {
    postconditions<Int> { result -> result > 0 }

    if (first) {
        return 1
    }
    return 2
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>branchEarlyReturnsBoundary<!>(first: Boolean): Int {
    postconditions<Int> { result -> result > 0 }

    if (first) {
        return 1
    }
    return 0
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>nestedEarlyReturns<!>(outer: Boolean, inner: Boolean): Int {
    postconditions<Int> { result -> result > 0 }

    if (outer) {
        if (inner) {
            return 1
        }
        return 2
    }
    return 3
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>nestedEarlyReturnsBoundary<!>(outer: Boolean, inner: Boolean): Int {
    postconditions<Int> { result -> result > 0 }

    if (outer) {
        if (inner) {
            return 1
        }
        return 0
    }
    return 2
}<!>

@AlwaysVerify
fun <!VIPER_TEXT!>returnBranchExpression<!>(first: Boolean): Int {
    postconditions<Int> { result -> result > 0 }
    return if (first) 1 else 2
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>returnBranchExpressionBoundary<!>(first: Boolean): Int {
    postconditions<Int> { result -> result > 0 }
    return if (first) 1 else 0
}<!>

@NeverConvert
inline fun earlyReturnStep(block: () -> Unit) {
    block()
}

@AlwaysVerify
fun <!VIPER_TEXT!>inlineLambdaEarlyReturn<!>(exitFromLambda: Boolean): Int {
    postconditions<Int> { result -> result > 0 }

    earlyReturnStep {
        if (exitFromLambda) {
            return 1
        }
    }
    return 2
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>inlineLambdaEarlyReturnBoundary<!>(exitFromLambda: Boolean): Int {
    postconditions<Int> { result -> result > 0 }

    earlyReturnStep {
        if (exitFromLambda) {
            return 0
        }
    }
    return 1
}<!>
