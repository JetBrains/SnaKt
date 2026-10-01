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

class SiliconFrontendTest {
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
