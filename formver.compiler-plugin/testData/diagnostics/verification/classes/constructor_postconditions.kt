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

// A property initialized by a constructor call has no pure value, so no postcondition can be
// inferred for it. Inference must skip it (not crash the pure linearizer) while still recovering
// the pure facts about the other properties.
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
    verify(fresh.argument == 42)
    verify(fresh.tag == 7)
}
