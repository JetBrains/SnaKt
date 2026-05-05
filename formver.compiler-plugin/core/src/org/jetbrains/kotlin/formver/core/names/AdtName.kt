/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
