package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.KtSourceElement

/**
 * Thrown by [UniqueCheckVisitor] when it detects a violation of the uniqueness/ownership rules.
 *
 * [source] carries the [KtSourceElement] of the offending expression so that
 * [UniqueDeclarationChecker] can report the diagnostic at the correct position in the source file.
 * It may be `null` when no source information is available.
 */
class UniquenessCheckException(val source: KtSourceElement?, override val message: String) : Exception()