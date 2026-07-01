package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.INVALID_DUPLICATE_UNIQUE_ARGUMENT
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.INVALID_OVERLAPPING_UNIQUE_ARGUMENTS

object CallArgumentUniquenessCollisionChecker
    : FirCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val arguments = CallParametersUniquenessResolver.mapArgumentTypeFactsOf(expression)
        val uniqueArguments = arguments
            .filter { (_, uniqueness) -> uniqueness == Uniqueness.Unique }
            .map { (expression, _) -> expression }

        for ((index, firstArgument) in uniqueArguments.withIndex()) {
            val firstAccessState = firstArgument.resolveAccessState()

            for (secondArgument in uniqueArguments.subList(index + 1, uniqueArguments.size)) {
                val secondAccessState = secondArgument.resolveAccessState()

                for (firstPath in firstAccessState.enumeratePaths()) {
                    if (firstPath.uniqueness != Uniqueness.Unique) continue

                    for (secondPath in secondAccessState.enumeratePaths()) {
                        if (firstPath.uniqueness != Uniqueness.Unique) continue

                        var commonPrefix = 0

                        while (commonPrefix < firstPath.size && commonPrefix < secondPath.size) {
                            if (firstPath[commonPrefix] == secondPath[commonPrefix]) {
                                commonPrefix++
                            } else {
                                break
                            }
                        }

                        if (commonPrefix > 0) {
                            if (commonPrefix == firstPath.size && commonPrefix == secondPath.size) {
                                reporter.reportOn(
                                    firstArgument.source,
                                    INVALID_DUPLICATE_UNIQUE_ARGUMENT,
                                    firstPath
                                )
                                reporter.reportOn(
                                    secondArgument.source,
                                    INVALID_DUPLICATE_UNIQUE_ARGUMENT,
                                    secondPath
                                )
                            } else {
                                reporter.reportOn(
                                    firstArgument.source,
                                    INVALID_OVERLAPPING_UNIQUE_ARGUMENTS,
                                    firstPath,
                                    secondPath
                                )
                                reporter.reportOn(
                                    secondArgument.source,
                                    INVALID_OVERLAPPING_UNIQUE_ARGUMENTS,
                                    secondPath,
                                    firstPath
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
