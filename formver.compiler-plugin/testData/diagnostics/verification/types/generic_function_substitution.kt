// FULL_JDK
@file:Suppress("NOTHING_TO_INLINE")

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
inline fun <T> substitutionIdentity(value: T): T = value

@NeverConvert
inline fun <First, Second> substitutionSecond(first: First, second: Second): Second = second

@NeverConvert
inline fun <T : Number> boundedSubstitutionIdentity(value: T): T = value

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>inferredAndExplicitSubstitution<!>(): Boolean {
    contract {
        returns(true)
    }

    val inferred = substitutionIdentity(1)
    val explicit = substitutionIdentity<Int>(1)
    return inferred == 1 && explicit == 1
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>explicitSubstitutionPair<!>(): Boolean {
    contract {
        returns(true)
    }

    val specific = substitutionIdentity<Int>(1)
    val general = substitutionIdentity<Any>(1)
    return specific == 1 && general == 1
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>multipleTypeParameterSubstitution<!>(): Boolean {
    contract {
        returns(true)
    }

    val specific = substitutionSecond<Int, Boolean>(1, true)
    val general = substitutionSecond<Any, Any>(1, true)
    return specific && general == true
}

@OptIn(ExperimentalContracts::class)
fun <!VIPER_TEXT!>boundedReturnSubstitution<!>(): Boolean {
    contract {
        returns(true)
    }

    val specific = boundedSubstitutionIdentity<Int>(1)
    val boundary = boundedSubstitutionIdentity<Number>(1)
    return specific == 1 && boundary == 1
}
