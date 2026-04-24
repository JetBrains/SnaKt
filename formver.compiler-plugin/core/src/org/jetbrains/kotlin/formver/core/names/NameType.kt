package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.NameTypeBase

/**
 * Collects all types of names we can have.
 */
sealed interface NameType : NameTypeBase {
    override val inViper: Boolean
        get() = false

    enum class Member : NameType {
        Property, BackingField, Getter, Setter,
        ExtensionSetter, ExtensionGetter
    }

    enum class Base : NameType {
        Constructor, Function, Predicate,
        Havoc, Variable, Domain, DomainFunction, Label
    }

    enum class TypeCategory : NameType {
        Class, GeneralType
    }

}
