/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class UniquenessResolver(val session: FirSession) {
    
    private fun getAnnotationId(name: String): ClassId =
        ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")),
            Name.identifier(name)
        )

    private val uniqueId: ClassId
        get() = getAnnotationId("Unique")

    private val borrowId: ClassId
        get() = getAnnotationId("Borrowed")

    fun resolveUniqueLevel(element: FirAnnotationContainer): UniqueLevel =
        if (element.hasAnnotation(uniqueId, session))
            UniqueLevel.Unique
        else
            UniqueLevel.Shared

    fun resolveBorrowLevel(element: FirAnnotationContainer): BorrowLevel =
        if (element.hasAnnotation(borrowId, session))
            BorrowLevel.Borrowed
        else
            BorrowLevel.Free

    fun resolveUniquenessType(element: FirAnnotationContainer): UniquenessType  =
        UniquenessType.Active(
            resolveUniqueLevel(element),
            resolveBorrowLevel(element)
        )

    fun resolveUniquenessType(element: FirElement): UniquenessType? =
        if (element is FirAnnotationContainer)
            resolveUniquenessType(element)
        else
            null

}