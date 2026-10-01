// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Positive control: the implementation establishes the exact postcondition.
@AlwaysVerify
fun <!VIPER_TEXT!>exactIncrement<!>(x: Int): Int {
    postconditions<Int> { result -> result == x + 1 }
    return x + 1
}

// Negative mutation: negating that postcondition contradicts every return value.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>negatedIncrementPostcondition<!>(x: Int): Int {
    postconditions<Int> { result -> result != x + 1 }
    return x + 1
}<!>

// Positive boundary control: the inclusive upper bound admits x == 3.
@AlwaysVerify
fun <!VIPER_TEXT!>inclusiveUpperBound<!>(x: Int) {
    preconditions { 0 <= x && x <= 3 }
    verify(x <= 3)
}

// Negative mutation: shifting <= 3 to < 3 fails specifically at the admitted boundary.
@AlwaysVerify
fun <!VIPER_TEXT!>shiftedUpperBound<!>(x: Int) {
    preconditions { 0 <= x && x <= 3 }
    verify(<!VIPER_VERIFICATION_ERROR!>x < 3<!>)
}

// Positive control: the precondition proves that index zero is in bounds.
@AlwaysVerify
fun <!VIPER_TEXT!>firstCharacterWithPrecondition<!>(text: String): Char {
    preconditions { text.length > 0 }
    return text[0]
}

// Negative mutation: removing the non-empty precondition leaves the index access unsafe.
@AlwaysVerify
fun <!VIPER_TEXT!>firstCharacterWithoutPrecondition<!>(text: String): Char {
    return <!VIPER_VERIFICATION_ERROR!>text[0]<!>
}

// Positive control: each branch returns the value described by the postcondition.
@AlwaysVerify
fun <!VIPER_TEXT!>branchResult<!>(chooseFirst: Boolean): Int {
    postconditions<Int> { result ->
        (chooseFirst implies (result == 1)) &&
            ((!chooseFirst) implies (result == 2))
    }
    return if (chooseFirst) 1 else 2
}

// Negative mutation: swapping branch results violates both postcondition implications.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>swappedBranchResult<!>(chooseFirst: Boolean): Int {
    postconditions<Int> { result ->
        (chooseFirst implies (result == 1)) &&
            ((!chooseFirst) implies (result == 2))
    }
    return if (chooseFirst) 2 else 1
}<!>
