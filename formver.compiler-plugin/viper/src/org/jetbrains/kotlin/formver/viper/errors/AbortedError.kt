/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.Position

/**
 * An error that stopped verification, such as a Silver consistency error or an exception thrown inside Silicon.
 * The program was not fully verified, so it must not be treated as verified.
 */
class AbortedError(val error: viper.silver.verifier.AbstractError) : VerifierError {
    override val id: String
        get() = error.fullId()
    override val msg: String
        get() = error.readableMessage()
    override val position: Position
        get() = Position.fromSilver(error.pos())
}
