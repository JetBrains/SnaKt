package org.jetbrains.kotlin.formver.core.names.shortNameResolver

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.SymbolicName


abstract class ViperKeyword(keyword: String) : AnyName {
    override val inViper: Boolean = true

    override val candidates: List<CandidateName> = nameOnlyCandidates(keyword)

    override val children: List<AnyName> = emptyList()
}

object FunctionResultVariableName : ViperKeyword("result"), SymbolicName {
    override val inViper: Boolean = true
    override val candidates: List<CandidateName> = nameOnlyCandidates("result")
    override val children: List<AnyName> = emptyList()
}

object ViperKeywords {
    val keywords = setOf(
        "import",
        "define",
        "field",
        "method",
        "function",
        "predicate",
        "domain",
        "interpretation",
        "returns",
        "unique",
        "requires",
        "ensures",
        "invariant",
        "forall",
        "forperm",
        "new",
        "lhs",
        "if",
        "elseif",
        "else",
        "while",
        "fold",
        "unfold",
        "inhale",
        "package",
        "assert",
        "assume",
        "var",
        "label",
        "goto",
        "quasihavoc",
        "quasihavocall",
        "true",
        "false",
        "null",
        "none",
        "wildcard",
        "write",
        "epsilon",
    ).map { object : ViperKeyword(it) {} } + listOf(
        FunctionResultVariableName
    )
}
