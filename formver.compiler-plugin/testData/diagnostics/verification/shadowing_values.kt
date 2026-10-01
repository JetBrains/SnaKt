// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions
import org.jetbrains.kotlin.formver.plugin.verify

@AlwaysVerify
fun <!VIPER_TEXT!>shadowedParameterAndNestedLocals<!>(value: Int): Int {
    preconditions {
        value == 11
    }
    postconditions<Int> { result ->
        result == 112337
    }

    val parameterValue = value
    val value = 23
    var result = parameterValue * 10000 + value * 100
    if (true) {
        val value = 37
        result = result + value
        verify(value == 37)
    }
    verify(value == 23)
    verify(<!VIPER_VERIFICATION_ERROR!>value == parameterValue<!>)
    return result
}

class ShadowingValues(val value: Int) {
    @AlwaysVerify
    fun Int.<!VIPER_TEXT!>shadowedReceiversFieldAndParameter<!>(value: Int) {
        preconditions {
            this@ShadowingValues.value == 11
            this@shadowedReceiversFieldAndParameter == 23
            value == 37
        }

        val parameterValue = value
        val value = 41
        verify(
            this@ShadowingValues.value == 11,
            this@shadowedReceiversFieldAndParameter == 23,
            parameterValue == 37,
            value == 41,
        )
        verify(
            <!VIPER_VERIFICATION_ERROR!>this@shadowedReceiversFieldAndParameter ==
                    this@ShadowingValues.value<!>,
        )
    }
}
