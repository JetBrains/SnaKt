// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

object SingletonValue {
    val code: Int = 7
}

@AlwaysVerify
fun directSingletonObject() {
    verify(<!INTERNAL_ERROR!>SingletonValue<!> == SingletonValue)
    verify(SingletonValue.code == 7)
    verify(
        when (SingletonValue) {
            SingletonValue -> true
            <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> false
        }
    )
}

@AlwaysVerify
fun singletonObjectParameter(value: SingletonValue) {
    verify(value == <!INTERNAL_ERROR!>SingletonValue<!>)
    verify(value.code == 7)
    verify(
        when (value) {
            SingletonValue -> true
            <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> false
        }
    )
}
