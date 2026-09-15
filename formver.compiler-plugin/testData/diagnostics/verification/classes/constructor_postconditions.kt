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
