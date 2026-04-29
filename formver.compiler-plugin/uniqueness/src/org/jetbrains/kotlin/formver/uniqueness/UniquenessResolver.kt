/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves and determines the default uniqueness and borrowing levels of FIR symbols.
 *
 * @param session The FIR session used to access annotations and metadata associated with symbols.
 */
class UniquenessResolver(
    val session: FirSession
) {
    private fun getAnnotationId(name: String): ClassId =
        ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")),
            Name.identifier(name)
        )

    private val uniqueId: ClassId
        get() = getAnnotationId("Unique")

    private val borrowId: ClassId
        get() = getAnnotationId("Borrowed")

    fun resolveUniqueLevel(symbol: FirBasedSymbol<*>): UniqueLevel =
        if (symbol.hasAnnotation(uniqueId, session))
            UniqueLevel.Unique
        else
            UniqueLevel.Shared

    fun resolveBorrowLevel(symbol: FirBasedSymbol<*>): BorrowLevel =
        if (symbol.hasAnnotation(borrowId, session))
            BorrowLevel.Local
        else
            BorrowLevel.Global

    fun resolveUniquenessType(symbol: FirBasedSymbol<*>): UniquenessType.Active  =
        UniquenessType.Active(
            resolveUniqueLevel(symbol),
            resolveBorrowLevel(symbol)
        )
}
