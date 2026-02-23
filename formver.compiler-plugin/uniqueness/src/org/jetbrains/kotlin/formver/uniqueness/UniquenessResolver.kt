/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed interface HasAnnotation {
    class Symbol(val symbol: FirBasedSymbol<*>) : HasAnnotation {
        override fun hasAnnotation(id: ClassId, session: FirSession) = symbol.hasAnnotation(id, session)
    }

    class AnnotationContainer(val container: FirAnnotationContainer) : HasAnnotation {
        override fun hasAnnotation(id: ClassId, session: FirSession) = container.hasAnnotation(id, session)
    }

    fun hasAnnotation(id: ClassId, session: FirSession): Boolean
}

class UniquenessResolver(val session: FirSession) {
    private fun getAnnotationId(name: String): ClassId =
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")), Name.identifier(name))

    private val uniqueId: ClassId
        get() = getAnnotationId("Unique")

    private val borrowingId: ClassId
        get() = getAnnotationId("Borrowed")

    fun resolveUniqueLevel(declaration: HasAnnotation): UniqueLevel =
        if (declaration.hasAnnotation(uniqueId, session))
            UniqueLevel.Unique
        else
            UniqueLevel.Shared

    fun resolveBorrowLevel(declaration: HasAnnotation): BorrowLevel =
        if (declaration.hasAnnotation(borrowingId, session))
            BorrowLevel.Borrowed
        else
            BorrowLevel.Free

    fun resolveUniquenessType(declaration: HasAnnotation): UniquenessType.Active  =
        UniquenessType.Active(
            resolveUniqueLevel(declaration),
            resolveBorrowLevel(declaration)
        )
}

fun UniquenessResolver.resolveUniquenessType(declaration: FirBasedSymbol<*>): UniquenessType.Active =
    resolveUniquenessType(HasAnnotation.Symbol(declaration))

fun UniquenessResolver.resolveUniquenessType(declaration: FirAnnotationContainer): UniquenessType.Active =
    resolveUniquenessType(HasAnnotation.AnnotationContainer(declaration))