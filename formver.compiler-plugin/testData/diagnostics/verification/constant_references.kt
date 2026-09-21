// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

private const val ANSWER = 40 + 2
private const val GREETING = "Hello, " + "SnaKt"
private const val ENABLED = true

@AlwaysVerify
fun <!VIPER_TEXT!>constantReferences<!>() {
    verify(ANSWER == 42)
    verify(GREETING == "Hello, SnaKt")
    verify(ENABLED)
}
