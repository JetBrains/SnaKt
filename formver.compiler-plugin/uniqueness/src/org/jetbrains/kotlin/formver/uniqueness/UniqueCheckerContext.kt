/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.common.ErrorCollector
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.name.ClassId

/**
 * Uniform abstraction over the two FIR ways of checking for annotations: via a
 * [FirBasedSymbol] or via a [FirAnnotationContainer].
 *
 * The uniqueness checker sometimes holds symbols and sometimes annotation containers;
 * this sealed interface lets [UniqueCheckerContext.resolveUniqueAnnotation] and
 * [UniqueCheckerContext.resolveBorrowingAnnotation] accept either without overloading.
 */
sealed interface HasAnnotation {
    /**
     * Wraps a [FirBasedSymbol] for annotation lookup.
     */
    class Symbol(val symbol: FirBasedSymbol<*>) : HasAnnotation {
        override fun hasAnnotation(id: ClassId, session: FirSession) = symbol.hasAnnotation(id, session)
    }

    /**
     * Wraps a [FirAnnotationContainer] (e.g. a [org.jetbrains.kotlin.fir.declarations.FirDeclaration])
     * for annotation lookup.
     */
    class AnnotationContainer(val container: FirAnnotationContainer) : HasAnnotation {
        override fun hasAnnotation(id: ClassId, session: FirSession) = container.hasAnnotation(id, session)
    }

    /** Returns `true` if the annotation identified by [id] is present in [session]. */
    fun hasAnnotation(id: ClassId, session: FirSession): Boolean
}

/**
 * Environment provided to the ownership/uniqueness checker during analysis of a single function.
 *
 * Implementations (see [UniqueChecker]) supply annotation resolution and the shared
 * [ContextTrie] root that accumulates per-path ownership state as the checker walks
 * the FIR tree.
 */
interface UniqueCheckerContext {
    val config: PluginConfiguration
    val errorCollector: ErrorCollector
    val session: FirSession

    /** Returns the [UniqueLevel] declared on [declaration] via `@Unique`, or [UniqueLevel.Shared]. */
    fun resolveUniqueAnnotation(declaration: HasAnnotation): UniqueLevel

    /** Returns the [BorrowingLevel] declared on [declaration] via `@Borrowed`, or [BorrowingLevel.Plain]. */
    fun resolveBorrowingAnnotation(declaration: HasAnnotation): BorrowingLevel

    /**
     * Retrieves the [ContextTrie] node corresponding to the given path.
     * If the path does not exist in the current context, inserts the necessary nodes.
     *
     * @param path A list of [FirBasedSymbol] items representing a sequence of symbols forming a path in code (`x.y.z -> [local/x, A/y, B/z]`).
     *             Note that the first element in the path must be a local symbol.
     * @return The [ContextTrie] node at the end of the specified path.
     *         If intermediate nodes do not exist, they are created with unique levels extracted from annotations.
     */
    fun getOrPutPath(path: List<FirBasedSymbol<*>>): UniquePathContext
}

/** Convenience overload of [UniqueCheckerContext.resolveUniqueAnnotation] for [FirBasedSymbol]. */
fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirBasedSymbol<*>): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.Symbol(declaration))

/** Convenience overload of [UniqueCheckerContext.resolveUniqueAnnotation] for [FirAnnotationContainer]. */
fun UniqueCheckerContext.resolveUniqueAnnotation(declaration: FirAnnotationContainer): UniqueLevel =
    resolveUniqueAnnotation(HasAnnotation.AnnotationContainer(declaration))

/** Convenience overload of [UniqueCheckerContext.resolveBorrowingAnnotation] for [FirBasedSymbol]. */
fun UniqueCheckerContext.resolveBorrowingAnnotation(declaration: FirBasedSymbol<*>): BorrowingLevel =
    resolveBorrowingAnnotation(HasAnnotation.Symbol(declaration))

/** Convenience overload of [UniqueCheckerContext.resolveBorrowingAnnotation] for [FirAnnotationContainer]. */
fun UniqueCheckerContext.resolveBorrowingAnnotation(declaration: FirAnnotationContainer): BorrowingLevel =
    resolveBorrowingAnnotation(HasAnnotation.AnnotationContainer(declaration))