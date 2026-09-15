// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>canonicalQuantifierTrigger<!>(s: String): Boolean {
    preconditions {
        s.length > 0
        forAll<Int> { i ->
            triggers(s[i], i + 1)
            (0 <= i && i < s.length) implies (s[i] >= 'a')
        }
    }
    postconditions<Boolean> { it }
    return s[0] >= 'a'
}

@AlwaysVerify
fun <!VIPER_TEXT!>reorderedQuantifierConjuncts<!>(s: String): Boolean {
    preconditions {
        s.length > 0
        forAll<Int> { i ->
            triggers(s[i], i + 1)
            (i < s.length && 0 <= i) implies (s[i] >= 'a')
        }
    }
    postconditions<Boolean> { it }
    return s[0] >= 'a'
}

@AlwaysVerify
fun <!VIPER_TEXT!>renamedQuantifierBinder<!>(s: String): Boolean {
    preconditions {
        s.length > 0
        forAll<Int> { index ->
            triggers(s[index], index + 1)
            (0 <= index && index < s.length) implies (s[index] >= 'a')
        }
    }
    postconditions<Boolean> { it }
    return s[0] >= 'a'
}

@AlwaysVerify
fun <!VIPER_TEXT!>reorderedQuantifierTriggers<!>(s: String): Boolean {
    preconditions {
        s.length > 0
        forAll<Int> { index ->
            triggers(index + 1, s[index])
            (0 <= index && index < s.length) implies (s[index] >= 'a')
        }
    }
    postconditions<Boolean> { it }
    return s[0] >= 'a'
}

<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>strongerConclusionDoesNotVerify<!>(s: String): Boolean {
    preconditions {
        s.length > 0
        forAll<Int> { index ->
            triggers(index + 1, s[index])
            (index < s.length && 0 <= index) implies (s[index] >= 'a')
        }
    }
    postconditions<Boolean> { it }
    return s[0] > 'a'
}<!>
