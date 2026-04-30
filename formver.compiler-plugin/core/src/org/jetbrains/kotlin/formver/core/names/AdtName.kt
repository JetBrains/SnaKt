package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.SymbolicName

data class AdtName(val className: SymbolicName, val suffix: String? = null) : SymbolicName {
    override val nameType: NameType = NameType.Base.Adt
    override val inViper: Boolean = suffix == null
    override val candidates: List<CandidateName> = if (suffix == null)
        nameWithDependentPrefixCandidates(className, nameType)
    else buildCandidates {
        candidate { +suffix }
        candidate { +className; +suffix }
    }
    override val children: List<AnyName> = if (suffix == null) listOf(nameType, className) else listOf(className)
}

data class AdtConstructorName(val adtName: AdtName, val className: SymbolicName) : SymbolicName {
    override val nameType: NameType = NameType.Base.AdtCons
    override val inViper: Boolean = true
    override val candidates: List<CandidateName> = buildCandidates {
        candidate { +className }
        candidate { +adtName; +className }
        candidate { +nameType; +adtName; +className }
    }
    override val children: List<AnyName> = listOf(nameType, adtName, className)
}
