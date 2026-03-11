package org.jetbrains.kotlin.formver.common

import org.jetbrains.kotlin.KtSourceElement

/**
 * Internal exception thrown when the plugin encounters an unrecoverable error during conversion
 * or verification.
 *
 * Caught by `ViperPoweredDeclarationChecker` and reported as an `INTERNAL_ERROR` diagnostic.
 * Throwing this exception aborts conversion of the current function; compilation of other functions
 * continues normally.
 *
 * @param source The Kotlin source element closest to the error, used to attach the diagnostic to
 *               the correct source location. May be `null` if no source position is available.
 * @param message A description of the error, included in the diagnostic message.
 */
class SnaktInternalException(val source: KtSourceElement?, override val message: String) : Exception()
