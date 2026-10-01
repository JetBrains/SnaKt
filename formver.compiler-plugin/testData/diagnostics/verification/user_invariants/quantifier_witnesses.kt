// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>forallBooleanTautology<!>() {
    verify(forAll<Boolean> { value -> value || !value })
}

@AlwaysVerify
fun <!VIPER_TEXT!>forallBooleanCounterexample<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>forAll<Boolean> { value -> value }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanKnownWitness<!>() {
    // Known limitation (#297/#299): Silicon does not discover this direct witness.
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Boolean> { value -> value }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanImpossible<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Boolean> { value -> value && !value }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>forallIntegerTautology<!>() {
    verify(forAll<Int> { value -> value == value })
}

@AlwaysVerify
fun <!VIPER_TEXT!>forallIntegerCounterexample<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>forAll<Int> { value -> value != 0 }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsIntegerKnownWitness<!>() {
    // The same known witness-discovery limitation also affects the integer domain.
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Int> { value -> value == 0 }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsIntegerImpossible<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Int> { value -> value != value }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>forallVacuousIntegerRange<!>() {
    verify(forAll<Int> { value -> (0 <= value && value < 0) implies false })
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsEmptyIntegerRange<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Int> { value -> 0 <= value && value < 0 }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsSingletonIntegerRange<!>() {
    // A nonempty range separates witness discovery from the empty-range control above.
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Int> { value -> 0 <= value && value < 1 }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedForallTautology<!>() {
    verify(forAll<Boolean> { outer ->
        forAll<Boolean> { inner -> outer == outer && inner == inner }
    })
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedExistsKnownWitness<!>() {
    // Each outer Boolean has an equal inner witness, but the known limitation remains.
    verify(<!VIPER_VERIFICATION_ERROR!>forAll<Boolean> { outer ->
        exists<Boolean> { inner -> inner == outer }
    }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedQuantifierCounterexample<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Boolean> { outer ->
        forAll<Boolean> { inner -> inner == outer }
    }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>forallWithExplicitTrigger<!>() {
    verify(forAll<Int> { value ->
        triggers(value * value)
        value * value >= 0
    })
}

@AlwaysVerify
fun <!VIPER_TEXT!>forallWithExplicitTriggerCounterexample<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>forAll<Int> { value ->
        triggers(value * value)
        value * value > 0
    }<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsWithExplicitTriggerKnownWitness<!>() {
    // The trigger has no matching ground term, so it does not make the witness discoverable.
    verify(<!VIPER_VERIFICATION_ERROR!>exists<Int> { value ->
        triggers(value * value)
        value * value == 0
    }<!>)
}
