package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.SymbolicName

data class AdtName(val className: SymbolicName) : SymbolicName {
    override val nameType: NameType
        get() = NameType.Base.Adt
}

data class AdtConstructorName(val adtName: AdtName, val className: SymbolicName) : SymbolicName {
    override val nameType: NameType
        get() = NameType.Base.AdtCons
}

data class AdtInjectionName(val className: SymbolicName, val suffix: String) : SymbolicName