/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverridePropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.core.embeddings.properties.ClassPropertyAccess
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyAccessEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.asPropertyAccess
import org.jetbrains.kotlin.formver.core.isCustom
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

val FirIntersectionOverridePropertySymbol.propertyIntersections
    get() = intersections.filterIsInstanceAnd<FirPropertySymbol> { it.isVal == isVal }

/**
 * Finds a final property with default accessors through same-mutability intersection parents.
 * Assumes a Kotlin-valid property hierarchy: unrelated concrete implementations are disallowed
 * and final properties cannot override one another, so traversal order does not affect selection.
 * Symbol selection alone does not establish permissions to access the property.
 */
fun FirPropertySymbol.findFinalParentProperty(): FirPropertySymbol? =
    if (this !is FirIntersectionOverridePropertySymbol)
        (isFinal && !isCustom).ifTrue { this }
    else propertyIntersections.firstNotNullOfOrNull { it.findFinalParentProperty() }


/**
 * Embeds parameter, local, member and extension property accesses.
 * For dispatch-receiver properties, prefers a final, default-accessor declaration found
 * through intersection parents; otherwise embeds the referenced property symbol lazily.
 * The property embedding chooses its representation, which need not be a backing field
 * or include a setter.
 */
fun StmtConversionContext.embedPropertyAccess(accessExpression: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = accessExpression.calleeReference.symbol) {
        is FirValueParameterSymbol -> embedParameter(calleeSymbol).asPropertyAccess()
        is FirPropertySymbol -> {
            val type = embedType(calleeSymbol.resolvedReturnType)
            when {
                accessExpression.dispatchReceiver != null -> {
                    val property = calleeSymbol.findFinalParentProperty()?.let {
                        embedProperty(it)
                    } ?: embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.dispatchReceiver!!), property, type)
                }

                accessExpression.extensionReceiver != null -> {
                    val property = embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.extensionReceiver!!), property, type)
                }

                else -> embedLocalProperty(calleeSymbol)
            }
        }

        else ->
            error("Property access symbol $calleeSymbol has unsupported type.")
    }
