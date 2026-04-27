package org.jetbrains.kotlin.formver.core.names.shortNameResolver

import org.jetbrains.kotlin.formver.viper.AnyName


data class ViperKeyword(val keyword: String) : AnyName {
    override val inViper: Boolean = true
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
        "result",
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
    ).map { ViperKeyword(it) }
}
