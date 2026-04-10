package org.jetbrains.kotlin.formver.core.names.shortNameResolver

import org.jetbrains.kotlin.formver.viper.NamedEntity


class ViperKeyword(val keyword: String) : NamedEntity

object ViperKeywords {
    val keywords = mutableSetOf(
        "import",
        "define",
        "field",
        "method",
        "function",
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