/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.formver.core.annotationId

val uniquenessAnnotationId = annotationId("Unique")

fun FirDeclaration.hasUniquenessAnnotation(): Boolean =
    annotations.any { it.annotationTypeRef.coneType.classId == uniquenessAnnotationId }

/**
 * Extracts the uniqueness required by [this] declaration definition.
 */
fun FirDeclaration.extractUniqueness(): Uniqueness =
    if (hasUniquenessAnnotation()) {
        Uniqueness.Unique
    } else {
        Uniqueness.Shared
    }
