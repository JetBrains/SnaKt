package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.Position
import viper.silver.verifier.AbstractError

/** A backend failure that is neither an ordinary proof failure nor a consistency error. */
data class BackendError(val error: AbstractError) : VerifierError {
    override val id: String get() = error.fullId()
    override val msg: String get() = error.readableMessage()
    override val position: Position get() = Position.fromSilver(error.pos())
}
