package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * We also convert domain names and their function and axiom names as
 * they have to be globally unique as well.
 */

data class DomainName(val baseName: String) : SymbolicName {
    override val nameType: NameType
        get() = NameType.Base.Domain
}

data class UnqualifiedDomainFuncName(val baseName: String) : SymbolicName {
}

data class QualifiedDomainFuncName(val domainName: SymbolicName, val funcName: SymbolicName) : SymbolicName {
    override val nameType: NameType
        get() = NameType.Base.DomainFunction

}

data class NamedDomainAxiomLabel(val domainName: SymbolicName, val baseName: String) : SymbolicName
