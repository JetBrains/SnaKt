// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

// Independent truth-table oracle for implication: !antecedent || consequent.
@AlwaysVerify
fun <!VIPER_TEXT!>falseImpliesFalse<!>() {
    verify(!false || false)
}

@AlwaysVerify
fun <!VIPER_TEXT!>falseImpliesTrue<!>() {
    verify(!false || true)
}

@AlwaysVerify
fun <!VIPER_TEXT!>trueImpliesFalse<!>() {
    verify(<!VIPER_VERIFICATION_ERROR!>!true || false<!>)
}

@AlwaysVerify
fun <!VIPER_TEXT!>trueImpliesTrue<!>() {
    verify(!true || true)
}
