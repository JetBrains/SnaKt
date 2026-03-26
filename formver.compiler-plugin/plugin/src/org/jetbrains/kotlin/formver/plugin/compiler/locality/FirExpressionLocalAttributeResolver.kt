package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSession.Companion.sessionComponentAccessor
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.plugin.compiler.fir.declaration
import org.jetbrains.kotlin.formver.plugin.compiler.fir.tails
import java.util.WeakHashMap

class ConeLocalAttributeResolver(
    session: FirSession,
    private val cache: WeakHashMap<FirExpression, ConeLocalAttribute?>
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(): Factory = Factory { session ->
            ConeLocalAttributeResolver(session, WeakHashMap())
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext)
    fun resolve(expression: FirExpression): ConeLocalAttribute? {
        return cache.computeIfAbsent(expression) {
            expression.tails.fold(null) { result, tail ->
                val variable = (tail.toResolvedCallableSymbol(context.session) as? FirVariableSymbol)?.fir

                if (variable != null) {
                    result.union(variable.resolvedLocalAttribute)
                } else {
                    result.union(tail.declaredLocalAttribute)
                }
            }
        }
    }
}

val FirSession.coneLocalAttributeResolver: ConeLocalAttributeResolver by sessionComponentAccessor()

val FirExpression.declaredLocalAttribute: ConeLocalAttribute?
    get() = resolvedType.localAttribute

context(context: CheckerContext)
val FirExpression.resolvedLocalAttribute: ConeLocalAttribute?
    get() = context.session.coneLocalAttributeResolver.resolve(this)

context(context: CheckerContext)
val FirVariable.resolvedLocalAttribute: ConeLocalAttribute?
    get() = returnTypeRef.coneType.localAttribute?.copy(declaration = declaration)
