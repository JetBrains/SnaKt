package org.jetbrains.kotlin.formver.core.names.shortNameResolver

import org.jetbrains.kotlin.formver.viper.AnyName
import org.jetbrains.kotlin.formver.viper.CandidateName


fun nameOnlyCandidates(name: String): List<CandidateName> =
    buildCandidates { candidate { +name } }

fun nameWithPrefixAndSuffixCandidates(name: String, nameType: AnyName, suffix: String): List<CandidateName> =
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

fun nameWithPrefixCandidates(name: String, nameType: AnyName): List<CandidateName> =
    buildCandidates {
        candidate {
            +name
        }
        candidate {
            +nameType
            +name
        }
    }

fun nameWithDependentPrefixCandidates(name: String, prefix: AnyName): List<CandidateName> =
    buildCandidates {
        candidate { +name }
        candidate {
            +prefix
            +name
        }
    }

fun nameWithDependentPrefixCandidates(name: AnyName, prefix: AnyName): List<CandidateName> =
    buildCandidates {
        candidate { +name }
        candidate {
            +prefix
            +name
        }
    }

fun AnyName.candidates(): List<CandidateName> = candidates
