package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.CandidateName
import org.jetbrains.kotlin.formver.viper.NamedEntity
import org.jetbrains.kotlin.formver.viper.buildCandidates


class ViperKeyword(val keyword: String) : NamedEntity {

    override val candidates: List<CandidateName> = buildCandidates {
        candidate { +keyword }
    }
}

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

