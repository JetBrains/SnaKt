/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import org.jetbrains.kotlin.formver.viper.errors.AbortedError
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import viper.silver.verifier.AbortedExceptionally
import viper.silver.verifier.Failure
import viper.silver.verifier.TimeoutOccurred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Info
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Type
import org.jetbrains.kotlin.formver.viper.ast.silverNoTrafos

class SiliconFrontendTest {
    @Test
    fun `malformed Silver aborts without invoking the backend`() = with(object : NameResolver {
        override fun lookup(name: SymbolicName): String = error("No symbolic names in this fixture")
        override fun register(name: AnyName) = error("No symbolic names in this fixture")
        override fun resolve() = Unit
    }) {
        // An integer body cannot implement a Boolean function, regardless of Kotlin lowering support.
        val function = viper.silver.ast.Function(
            "invalid", emptyList<viper.silver.ast.LocalVarDecl>().toScalaSeq(), Type.Bool.toSilver(),
            emptyList<viper.silver.ast.Exp>().toScalaSeq(), emptyList<viper.silver.ast.Exp>().toScalaSeq(),
            Exp.IntLit(1).toSilver().toScalaOption(),
            Position.NoPosition.toSilver(), Info.NoInfo.toSilver(), silverNoTrafos,
        )
        val program = viper.silver.ast.Program(
            emptyList<viper.silver.ast.Domain>().toScalaSeq(), emptyList<viper.silver.ast.Field>().toScalaSeq(),
            listOf(function).toScalaSeq(), emptyList<viper.silver.ast.Predicate>().toScalaSeq(),
            emptyList<viper.silver.ast.Method>().toScalaSeq(), emptyList<viper.silver.ast.ExtensionMember>().toScalaSeq(),
            Position.NoPosition.toSilver(), Info.NoInfo.toSilver(), silverNoTrafos,
        )
        val errors = mutableListOf<VerifierError>()

        verifyConsistentProgram(program, { error("Malformed AST reached the backend") }, errors::add)

        assertTrue(errors.isNotEmpty())
        errors.forEach { assertIs<AbortedError>(it) }
    }

    @Test
    fun `backend abort and timeout are reported as aborted errors`() {
        val abort = AbortedExceptionally(IllegalStateException("prover failed"))
        val timeout = TimeoutOccurred(1, "second")
        val errors = mutableListOf<VerifierError>()

        reportFailures(Failure(listOf(abort, timeout).toScalaSeq()), errors::add)

        assertEquals(2, errors.size)
        assertIs<AbortedError>(errors[0])
        assertEquals(abort.readableMessage(), errors[0].msg)
        assertIs<AbortedError>(errors[1])
        assertEquals(timeout.readableMessage(), errors[1].msg)
    }
}
