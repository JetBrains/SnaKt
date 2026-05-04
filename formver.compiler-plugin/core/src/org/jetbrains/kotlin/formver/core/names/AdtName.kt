package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.SymbolicName

data class AdtName(val className: SymbolicName) : SymbolicName {
    override val nameType: NameType = NameType.Base.Adt
    override val inViper: Boolean = true
    override val candidates: List<CandidateName> = buildCandidates {
        candidate { +"adt"; +className }
    }
    override val children: List<AnyName> = listOf(nameType, className)
}

data class AdtConstructorName(val adtName: AdtName, val className: SymbolicName) : SymbolicName {
    override val nameType: NameType = NameType.Base.AdtCons
    override val inViper: Boolean = true
    override val candidates: List<CandidateName> = buildCandidates {
        candidate { +"constr"; +className }
    }
    override val children: List<AnyName> = listOf(nameType, adtName, className)
}
