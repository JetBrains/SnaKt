/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression

import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain.Companion.intInjection
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain.Companion.stringInjection
import org.jetbrains.kotlin.formver.core.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction

object OperatorExpEmbeddings {

    private val intIntToIntType
        get() = buildFunctionPretype {
            withParam { int() }
            withParam { int() }
            withReturnType { int() }
        }

    val AddIntInt = buildBinaryOperator {
        setName("plusInts")
        setSignature(intIntToIntType)
        viperImplementation { Exp.Add(args[0], args[1], pos, info) }
    }

    val SubIntInt = buildBinaryOperator {
        setName("minusInts")
        setSignature(intIntToIntType)
        viperImplementation { Exp.Sub(args[0], args[1], pos, info) }
    }

    val MulIntInt = buildBinaryOperator {
        setName("timesInts")
        setSignature(intIntToIntType)
        viperImplementation { Exp.Mul(args[0], args[1], pos, info) }
    }

    val DivIntInt = buildBinaryOperator {
        setName("divInts")
        setSignature(intIntToIntType)
        // Viper `/` is Euclidean: it rounds so that the remainder is nonnegative. Kotlin `/` truncates
        // towards zero. The two agree on nonnegative operands, so this relies on
        // `a / b == sign(a) * sign(b) * (|a| / |b|)` for `b != 0`. Viper integers are unbounded, so
        // `Int.MIN_VALUE / -1` is 2^31 here rather than wrapping to `Int.MIN_VALUE` as in Kotlin.
        viperImplementation {
            val dividendIsNegative = isNegative(args[0])
            val divisorIsNegative = isNegative(args[1])
            val magnitude = Exp.Div(absolute(args[0]), absolute(args[1]), pos, info)
            Exp.TernaryExp(
                Exp.NeCmp(dividendIsNegative, divisorIsNegative, pos, info),
                Exp.Minus(magnitude, pos, info),
                magnitude,
                pos,
                info,
            )
        }
        additionalConditions {
            precondition {
                intInjection.fromRef(args[1]) ne 0.toExp()
            }
        }
    }

    val RemIntInt = buildBinaryOperator {
        setName("remInts")
        setSignature(intIntToIntType)
        // Viper `%` is Euclidean: its result is always nonnegative. Kotlin `%` truncates, so its result
        // takes the sign of the dividend. The two agree on nonnegative operands, so this relies on
        // `a % b == sign(a) * (|a| mod |b|)` for `b != 0`. Viper integers are unbounded, so negating
        // `Int.MIN_VALUE` here does not overflow.
        viperImplementation {
            val magnitude = Exp.Mod(absolute(args[0]), absolute(args[1]), pos, info)
            Exp.TernaryExp(isNegative(args[0]), Exp.Minus(magnitude, pos, info), magnitude, pos, info)
        }
        additionalConditions {
            precondition {
                intInjection.fromRef(args[1]) ne 0.toExp()
            }
        }
    }

    private fun OperatorExpEmbeddingBuilder.ViperCallData.isNegative(exp: Exp): Exp =
        Exp.LtCmp(exp, Exp.IntLit(0, pos, info), pos, info)

    private fun OperatorExpEmbeddingBuilder.ViperCallData.absolute(exp: Exp): Exp =
        Exp.TernaryExp(isNegative(exp), Exp.Minus(exp, pos, info), exp, pos, info)

    private val intIntToBooleanType
        get() = buildFunctionPretype {
            withParam { int() }
            withParam { int() }
            withReturnType { boolean() }
        }

    val LeIntInt = buildBinaryOperator {
        setName("leInts")
        setSignature(intIntToBooleanType)
        viperImplementation { Exp.LeCmp(args[0], args[1], pos, info) }
    }

    val LtIntInt = buildBinaryOperator {
        setName("ltInts")
        setSignature(intIntToBooleanType)
        viperImplementation { Exp.LtCmp(args[0], args[1], pos, info) }
    }

    val GeIntInt = buildBinaryOperator {
        setName("geInts")
        setSignature(intIntToBooleanType)
        viperImplementation { Exp.GeCmp(args[0], args[1], pos, info) }
    }

    val GtIntInt = buildBinaryOperator {
        setName("gtInts")
        setSignature(intIntToBooleanType)
        viperImplementation { Exp.GtCmp(args[0], args[1], pos, info) }
    }

    val NegInt = buildUnaryOperator {
        setName("negInt")
        withSignature {
            withParam { int() }
            withReturnType { int() }
        }
        viperImplementation { Exp.Minus(args[0], pos, info) }
    }

    val Not = buildUnaryOperator {
        setName("notBool")
        withSignature {
            withParam { boolean() }
            withReturnType { boolean() }
        }
        viperImplementation { Exp.Not(args[0], pos, info) }
    }

    private val booleanBooleanToBooleanType
        get() = buildFunctionPretype {
            withParam { boolean() }
            withParam { boolean() }
            withReturnType { boolean() }
        }

    val And = buildBinaryOperator {
        setName("andBools")
        setSignature(booleanBooleanToBooleanType)
        viperImplementation { Exp.And(args[0], args[1], pos, info) }
    }

    val Or = buildBinaryOperator {
        setName("orBools")
        setSignature(booleanBooleanToBooleanType)
        viperImplementation { Exp.Or(args[0], args[1], pos, info) }
    }

    val Xor = buildBinaryOperator {
        setName("xorBools")
        setSignature(booleanBooleanToBooleanType)
        viperImplementation { Exp.NeCmp(args[0], args[1], pos, info) }
    }

    val Implies = buildBinaryOperator {
        setName("impliesBools")
        setSignature(booleanBooleanToBooleanType)
        viperImplementation { Exp.Implies(args[0], args[1], pos, info) }
    }

    val SubCharChar = buildBinaryOperator {
        setName("subChars")
        withSignature {
            withParam { char() }
            withParam { char() }
            withReturnType { int() }
        }
        viperImplementation { Exp.Sub(args[0], args[1], pos, info) }
    }

    private val charIntToCharType = buildFunctionPretype {
        withParam { char() }
        withParam { int() }
        withReturnType { char() }
    }

    val AddCharInt = buildBinaryOperator {
        setName("addCharInt")
        setSignature(charIntToCharType)
        viperImplementation { Exp.Add(args[0], args[1], pos, info) }
    }

    val SubCharInt = buildBinaryOperator {
        setName("subCharInt")
        setSignature(charIntToCharType)
        viperImplementation { Exp.Sub(args[0], args[1], pos, info) }
    }

    private val charCharToBooleanType = buildFunctionPretype {
        withParam { char() }
        withParam { char() }
        withReturnType { boolean() }
    }

    val GeCharChar = buildBinaryOperator {
        setName("geChars")
        setSignature(charCharToBooleanType)
        viperImplementation { Exp.GeCmp(args[0], args[1], pos, info) }
    }

    val GtCharChar = buildBinaryOperator {
        setName("gtChars")
        setSignature(charCharToBooleanType)
        viperImplementation { Exp.GtCmp(args[0], args[1], pos, info) }
    }

    val LeCharChar = buildBinaryOperator {
        setName("leChars")
        setSignature(charCharToBooleanType)
        viperImplementation { Exp.LeCmp(args[0], args[1], pos, info) }
    }

    val LtCharChar = buildBinaryOperator {
        setName("ltChars")
        setSignature(charCharToBooleanType)
        viperImplementation { Exp.LtCmp(args[0], args[1], pos, info) }
    }

    val StringLength = buildUnaryOperator {
        setName("stringLength")
        withSignature {
            withParam { string() }
            withReturnType { int() }
        }
        viperImplementation { Exp.SeqLength(args[0], pos, info) }
    }

    val StringGet = buildBinaryOperator {
        setName("stringGet")
        withSignature {
            withParam { string() }
            withParam { int() }
            withReturnType { char() }
        }
        viperImplementation { Exp.SeqIndex(args[0], args[1], pos, info) }
        additionalConditions {
            precondition {
                listOf(
                    intInjection.fromRef(args[1]) ge 0.toExp(),
                    intInjection.fromRef(args[1]) lt Exp.SeqLength(stringInjection.fromRef(args[0]))
                ).toConjunction()
            }
        }
    }

    val AddStringString = buildBinaryOperator {
        setName("addStrings")
        withSignature {
            withParam { string() }
            withParam { string() }
            withReturnType { string() }
        }
        viperImplementation { Exp.SeqAppend(args[0], args[1], pos, info) }
    }

    val AddStringChar = buildBinaryOperator {
        setName("addStringChar")
        withSignature {
            withParam { string() }
            withParam { char() }
            withReturnType { string() }
        }
        viperImplementation { Exp.SeqAppend(args[0], Exp.ExplicitSeq(listOf(args[1])), pos, info) }
    }

    val allTemplates
        get() = listOf(
            AddIntInt, SubIntInt, MulIntInt, DivIntInt, RemIntInt, NegInt,
            LeIntInt, GeIntInt, LtIntInt, GtIntInt,
            Not, And, Or, Implies, Xor,
            AddCharInt, SubCharChar, SubCharInt,
            LeCharChar, GeCharChar, LtCharChar, GtCharChar,
            StringLength, StringGet, AddStringString, AddStringChar,
        )
}
