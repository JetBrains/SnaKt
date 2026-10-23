// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

class InitializedPrimary(val argument: Int) {
    val initialized = 17
}

class InitializedSecondary {
    val argument: Int
    val initialized = 23

    constructor(argument: Int) {
        this.argument = argument
    }
}

class DelegatingSecondary(val argument: Int) {
    val initialized = 29

    constructor() : this(42)
}

// Allocation alone does not establish an effect or nonescape guarantee. Unknown initializer
// effects disable all inferred equalities, not just the equality for the allocated property.
class NonPureInitializer(val argument: Int) {
    val tag = 7
    val nested = InitializedPrimary(argument)
}

@AlwaysVerify
fun <!VIPER_TEXT!>primaryConstructorPostconditions<!>() {
    val fresh = InitializedPrimary(42)
    verify(fresh.argument == 42)
    verify(fresh.initialized == 17)
}

@AlwaysVerify
fun <!VIPER_TEXT!>secondaryConstructorPostconditions<!>() {
    val fresh = InitializedSecondary(42)
    verify(fresh.argument == 42)
    verify(fresh.initialized == 23)
}

@AlwaysVerify
fun <!VIPER_TEXT!>delegatingConstructorPostconditions<!>() {
    val fresh = DelegatingSecondary()
    verify(fresh.argument == 42)
    verify(fresh.initialized == 29)
}

@AlwaysVerify
fun <!VIPER_TEXT!>nonPureInitializerPostconditions<!>() {
    val fresh = NonPureInitializer(42)
    verify(<!VIPER_VERIFICATION_ERROR!>fresh.argument == 42<!>)
    verify(<!VIPER_VERIFICATION_ERROR!>fresh.tag == 7<!>)
}

class InitMutation {
    var x = 1
    init { x = 2 }
}

class OtherReceiverWrite {
    var x = 0
    constructor(other: OtherReceiverWrite) { other.x = 5 }
    constructor()
}

class UnknownInitializerEffect {
    var x = 1
    val y = change()
    private fun change(): Int { x = 2; return 0 }
}

// These stale equalities must not follow from an abstract constructor call. The diagnostic
// expectations are source guards; execution-dependent Viper goldens still require validation.
@AlwaysVerify
fun initBlockDoesNotEstablishStaleState() {
    val fresh = InitMutation()
    verify(<!VIPER_VERIFICATION_ERROR!>fresh.x == 1<!>)
}

@AlwaysVerify
fun otherReceiverDoesNotEstablishConstructedState(other: OtherReceiverWrite) {
    val fresh = OtherReceiverWrite(other)
    verify(<!VIPER_VERIFICATION_ERROR!>fresh.x == 5<!>)
}

@AlwaysVerify
fun unknownInitializerDoesNotPreserveEarlierState() {
    val fresh = UnknownInitializerEffect()
    verify(<!VIPER_VERIFICATION_ERROR!>fresh.x == 1<!>)
}
