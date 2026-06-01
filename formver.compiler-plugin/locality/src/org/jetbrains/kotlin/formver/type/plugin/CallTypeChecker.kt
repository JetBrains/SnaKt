package org.jetbrains.kotlin.formver.type.plugin

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType

/**
 * Checker for type compatibility in function calls.
 *
 * @param Type the type class of the arguments.
 * @param typeJudgment the type judgment to use for checking type compatibility.
 * @param expressionTypeResolver the resolver for resolving the types of the arguments.
 * @param callParametersTypeResolver for resolving the types of the call's parameters.
 * @param contextDiagnosticFactory the diagnostic factory to use for reporting type incompatibility in context
 *  parameters.
 */
class CallTypeChecker<Type>(
    kind: MppCheckerKind,
    private val typeJudgment: TypeJudgment<Type>,
    private val expressionTypeResolver: ExpressionTypeResolver<Type>,
    private val callParametersTypeResolver: CallParametersTypeResolver<Type>,
    private val argumentDiagnosticFactory: KtDiagnosticFactory3<String, Type, Type>,
    private val contextDiagnosticFactory: KtDiagnosticFactory3<ConeKotlinType, Type, Type>
) : FirCallChecker(kind) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val requiredTypes = callParametersTypeResolver.resolveParameterTypesOf(expression)

        for ((argument, requiredType) in requiredTypes) {
            val effectiveArguments = argument.unwrapAndFlattenArgument(flattenArrays = false)

            for (effectiveArgument in effectiveArguments) {
                val actualType = expressionTypeResolver.resolveTypeOf(effectiveArgument)

                if (typeJudgment.satisfies(requiredType, actualType)) continue

                if (argument.source?.kind is KtFakeSourceElementKind.ImplicitContextParameterArgument) {
                    reporter.reportOn(
                        expression.source,
                        contextDiagnosticFactory,
                        argument.resolvedType,
                        requiredType,
                        actualType
                    )
                } else {
                    reporter.reportOn(
                        effectiveArgument.source ?: argument.source,
                        argumentDiagnosticFactory,
                        "Argument",
                        requiredType,
                        actualType
                    )
                }
            }
        }
    }
}
