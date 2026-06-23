package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression


/**
 * Resolves the receiver of [this] qualified access expression targeting a property.
 *
 * NOTE: this property shouldn't be used to find the receiver of a qualified access expression targeting anything other
 * than a plain property or local variable.
 */
val FirQualifiedAccessExpression.pathReceiver: FirExpression?
    get() = explicitReceiver ?: extensionReceiver ?: dispatchReceiver
