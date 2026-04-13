package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.core.names.shortNameResolver.ViperKeyword
import org.jetbrains.kotlin.formver.viper.NameType
import org.jetbrains.kotlin.formver.viper.NamedEntity
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.DomainName
import org.jetbrains.kotlin.formver.viper.ast.NamedDomainAxiomLabel
import org.jetbrains.kotlin.formver.viper.ast.QualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.ast.UnqualifiedDomainFuncName


private fun nameOnlyCandidates(name: Any): List<CandidateName> = buildCandidates { candidate { +name } }
private fun nameWithPrefixAndSuffixCandidates(name: Any, nameType: NameType, suffix: String): List<CandidateName> =
    buildCandidates {
        candidate {
            +name
        }
        candidate {
            +name
            +suffix
        }
        candidate {
            +nameType
            +name
            +suffix
        }
    }

private fun nameWithPrefixCandidates(name: Any, nameType: NameType): List<CandidateName> = buildCandidates {
    candidate {
        +name
    }
    candidate {
        +nameType
        +name
    }
}

private fun nameWithDependentPrefixCandidates(name: Any, prefix: NamedEntity): List<CandidateName> = buildCandidates {
    candidate { +name }
    candidate {
        +prefix
        +name
    }
}

private fun candidatesWithScope(name: Any, scope: NameScope, nameType: NameType? = null): List<CandidateName> =
    buildCandidates {
        +nameWithDependentPrefixCandidates(name, scope)
        if (nameType != null) {
            candidate {
                +nameType
                +scope
                +name
            }
        }
    }


fun NamedEntity.candidates(): List<CandidateName> = when (this) {
    is NameScope -> candidates()
    is SymbolicName -> candidates()
    is NameType -> candidates()
    is ViperKeyword -> candidates()
    else -> throw SnaktInternalException(null, "Unexpected name type: ${this::class.simpleName}")
}

fun ViperKeyword.candidates(): List<CandidateName> = nameOnlyCandidates(keyword)

fun NameScope.candidates(): List<CandidateName> = when (this) {
    is BadScope -> nameOnlyCandidates("<BAD>")
    is ClassScope -> nameWithDependentPrefixCandidates(className, parent)

    is FakeScope -> nameOnlyCandidates("fake")
    is LocalScope -> nameOnlyCandidates(listOf("l", "$level"))

    is PackageScope -> buildCandidates {
        val split = packageName.asString().split(".")
        split.indices.forEach { i -> candidate { +split.takeLast(i + 1) } }
        candidate {
            +"pkg"
            +split
        }
    }

    is ParameterScope -> nameOnlyCandidates("par")
    is PrivateScope -> nameWithDependentPrefixCandidates("private", parent)
    is PublicScope -> nameOnlyCandidates("public")
}

fun SymbolicName.candidates(): List<CandidateName> = when (this) {
    is FreshName -> candidates()
    is KotlinName -> candidates()
    is ScopedName -> candidatesWithScope(name, scope, nameType)
    is DomainName -> nameWithPrefixCandidates(baseName, nameType)
    is NamedDomainAxiomLabel -> nameWithDependentPrefixCandidates(baseName, domainName)
    is QualifiedDomainFuncName -> buildCandidates {
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

    is UnqualifiedDomainFuncName -> nameOnlyCandidates(baseName)
    is ListOfNames<*> -> nameWithPrefixCandidates(names, nameType)
    is NameOfType -> candidates()
    else -> throw SnaktInternalException(null, "Unexpected name type: ${this::class.simpleName}")
}

fun FreshName.candidates(): List<CandidateName> = when (this) {
    is AnonymousBuiltinName -> buildCandidates {
        candidate {
            +"anon"
        }
        candidate {
            +"anon"
            +"builtin"
        }
        candidate {
            +"anon"
            +"builtin"
            +n.toString()
        }
    }

    is AnonymousName -> nameWithPrefixAndSuffixCandidates("anon", nameType, n.toString())
    is LabelName -> {
        val name = when (this@candidates) {
            is BreakLabelName -> "break"
            is CatchLabelName -> "catch"
            is ContinueLabelName -> "cont"
            is ReturnLabelName -> "ret"
            is TryExitLabelName -> "tryExit"
        }
        nameWithPrefixAndSuffixCandidates(name, nameType, n.toString())
    }

    is PlaceholderArgumentName -> nameWithPrefixAndSuffixCandidates("arg", nameType, n.toString())
    is ReturnVariableName -> nameWithPrefixAndSuffixCandidates("ret", nameType, n.toString())
    is SsaVariableName -> nameWithPrefixAndSuffixCandidates(baseName, nameType, n.toString())
    is PredicateName -> nameWithPrefixCandidates(name, nameType)
    DispatchReceiverName -> nameWithPrefixCandidates("this", nameType)
    is DomainAssociatedFuncName -> nameWithPrefixCandidates(name, nameType)
    is DomainFuncParameterName -> nameWithPrefixCandidates(name, nameType)
    ExtensionReceiverName -> nameWithPrefixCandidates("this", nameType)
    FunctionResultVariableName -> nameWithPrefixCandidates("result", nameType)
    is HavocName -> buildCandidates {
        candidate {
            +nameType
        }
        candidate {
            +nameType
            +type.name
        }
    }
    PlaceholderReturnVariableName -> nameWithPrefixCandidates("ret", nameType)
    is SpecialFieldName -> nameWithPrefixCandidates(name, nameType)
}


fun KotlinName.candidates(): List<CandidateName> = when (this) {
    is ClassKotlinName -> nameWithPrefixCandidates(name.asString().split("."), nameType)
    is ConstructorKotlinName -> buildCandidates {
        candidate {
            +nameType
        }
        candidate {
            +nameType
            +type.returnType.name
        }
        candidate {
            +nameType
            +type.returnType.name
            +"args"
            +type.paramTypes.map { it.name }
        }
    }

    is SimpleKotlinName -> nameOnlyCandidates(name.asStringStripSpecialMarkers())
    is TypedKotlinName -> nameWithPrefixCandidates(name.asStringStripSpecialMarkers(), nameType)
    is TypedKotlinNameWithType -> buildCandidates {
        +nameWithPrefixCandidates(name.asStringStripSpecialMarkers(), nameType)
        candidate {
            +nameType
            +name.asStringStripSpecialMarkers()
            +type.name
        }
    }
}

fun NameOfType.candidates(): List<CandidateName> = when (this) {
    is FunctionTypeName -> buildCandidates {
        candidate {
            +returns
        }
        candidate {
            +"args"
            +args
            +"ret"
            +returns
        }
    }

    is PretypeName -> nameOnlyCandidates(name)
    is TypeName -> buildCandidates {
        candidate {
            +pretype.name
        }
        candidate {
            if (nullable) +"N"
            noSeparator
            +pretype.name
        }
    }
}


fun NameType.candidates(): List<CandidateName> = nameOnlyCandidates(
    when (this) {
        NameType.Member.Property -> "p"
        NameType.Member.BackingField -> "bf"
        NameType.Member.Getter -> "g"
        NameType.Member.Setter -> "s"
        NameType.Member.ExtensionSetter -> "es"
        NameType.Member.ExtensionGetter -> "eg"
        NameType.TypeCategory.GeneralType -> "t"
        NameType.TypeCategory.Class -> "c"
        NameType.Base.Constructor -> "con"
        NameType.Base.Function -> "f"
        NameType.Base.Predicate -> "pred"
        NameType.Base.Havoc -> "havoc"
        NameType.Base.Label -> "lbl"
        NameType.Base.Variable -> "v"
        NameType.Base.Domain -> "d"
        NameType.Base.DomainFunction -> "df"
    }
)