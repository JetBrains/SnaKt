/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.common

/**
 * Severity at which a failed proof is reported.
 *
 * [WARNING] reports failed proofs as warnings; compiler warnings-as-errors policies still apply.
 * [ERROR] reports them as compiler errors. Conversion, consistency and internal failures are
 * unaffected. Successful compilation at [WARNING] does not establish verification success.
 */
enum class VerificationErrorSeverity {
    WARNING,
    ERROR;

    companion object {
        @JvmStatic
        fun defaultBehaviour(): VerificationErrorSeverity = WARNING
    }
}
