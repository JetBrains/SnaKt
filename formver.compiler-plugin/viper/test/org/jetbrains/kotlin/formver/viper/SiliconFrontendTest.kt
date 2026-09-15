package org.jetbrains.kotlin.formver.viper

import org.jetbrains.kotlin.formver.viper.errors.BackendError
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import viper.silver.verifier.AbortedExceptionally
import viper.silver.verifier.Failure
import viper.silver.verifier.TimeoutOccurred

class SiliconFrontendTest {
    @Test
    fun `backend abort and timeout are reported as failures`() {
        val abort = AbortedExceptionally(IllegalStateException("prover failed"))
        val timeout = TimeoutOccurred(1, "second")
        val errors = mutableListOf<VerifierError>()

        reportFailures(Failure(listOf(abort, timeout).toScalaSeq()), errors::add)

        assertEquals(2, errors.size)
        assertIs<BackendError>(errors[0])
        assertEquals(abort.readableMessage(), errors[0].msg)
        assertIs<BackendError>(errors[1])
        assertEquals(timeout.readableMessage(), errors[1].msg)
    }
}
