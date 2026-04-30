package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * We also convert domain names and their function and axiom names as
 * they have to be globally unique as well.
 */

data class DomainName(val baseName: String) : SymbolicName {
    override val nameType: NameType = NameType.Base.Domain
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithPrefixCandidates(baseName, nameType)

    override val children: List<AnyName> = listOf(nameType)
}

data class UnqualifiedDomainFuncName(val baseName: String) : SymbolicName {
    override val inViper: Boolean = false

    override val candidates: List<CandidateName> = nameOnlyCandidates(baseName)

    override val children: List<AnyName> = emptyList()
}

data class QualifiedDomainFuncName(val domainName: SymbolicName, val funcName: SymbolicName) : SymbolicName {
    override val nameType: NameType = NameType.Base.DomainFunction
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = buildCandidates {
        candidate {
            +funcName
        }
        candidate {
            +domainName
            +funcName
        }
        candidate {
            +nameType
            +domainName
            +funcName
        }
    }

    override val children: List<AnyName> = listOfNotNull(domainName, funcName, nameType)
}

data class InjectionFuncName(val baseName: SymbolicName, val suffix: String) : SymbolicName {
    override val inViper: Boolean = false
    override val candidates: List<CandidateName> = buildCandidates {
        candidate { +suffix }
        candidate { +baseName; +suffix }
    }
    override val children: List<AnyName> = listOf(baseName)
}

data class NamedDomainAxiomLabel(val domainName: SymbolicName, val baseName: String) : SymbolicName {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameWithDependentPrefixCandidates(baseName, domainName)

    override val children: List<AnyName> = listOf(domainName)
}
