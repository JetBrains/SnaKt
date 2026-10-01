/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ProverPreflightTest {
    private val brokenEnv: (String) -> String? = { if (it == "Z3_EXE") "/nonexistent/z3" else null }

    @Test
    fun `rejects an unusable Z3 from the environment`() {
        assertThrows<ProverNotFoundException> { checkProverIsUsable(emptyList(), brokenEnv) }
        assertThrows<ProverNotFoundException> { checkProverIsUsable(listOf("--prover", "Z3"), brokenEnv) }
    }

    @Test
    fun `leaves an explicitly configured prover to Silicon`() {
        assertDoesNotThrow { checkProverIsUsable(listOf("--z3Exe", "/opt/z3/bin/z3"), brokenEnv) }
        assertDoesNotThrow { checkProverIsUsable(listOf("--z3Exe=/opt/z3/bin/z3"), brokenEnv) }
        assertDoesNotThrow { checkProverIsUsable(listOf("--prover", "cvc5"), brokenEnv) }
        assertDoesNotThrow { checkProverIsUsable(listOf("--prover=Z3-API"), brokenEnv) }
    }
}
