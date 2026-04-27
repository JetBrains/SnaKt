package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.names.shortNameResolver.nameOnlyCandidates
import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.NameTypeBase

/**
 * Collects all types of names we can have.
 */
sealed interface NameType : NameTypeBase {
    override val inViper: Boolean
        get() = false

    override val candidates: List<CandidateName>
        get() = nameOnlyCandidates(
            when (this) {
                Member.Property -> "p"
                Member.BackingField -> "bf"
                Member.Getter -> "g"
                Member.Setter -> "s"
                Member.ExtensionSetter -> "es"
                Member.ExtensionGetter -> "eg"
                TypeCategory.GeneralType -> "t"
                TypeCategory.Class -> "c"
                Base.Constructor -> "con"
                Base.Function -> "f"
                Base.Predicate -> "pred"
                Base.Havoc -> "havoc"
                Base.Label -> "lbl"
                Base.Variable -> "v"
                Base.Domain -> "d"
                Base.DomainFunction -> "df"
            }
        )

    override val children: List<AnyName>
        get() = emptyList()

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
