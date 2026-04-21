/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.extensions.FirTypeAttributeExtension
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.formver.core.annotationId

private val localityAnnotationId = annotationId("Borrowed")

class LocalityAttributeExtension(
    session: FirSession
) : FirTypeAttributeExtension(session) {
    companion object {
        fun getFactory(): Factory {
            return Factory { session -> LocalityAttributeExtension(session) }
        }
    }

    override fun extractAttributeFromAnnotation(annotation: FirAnnotation): ConeAttribute<*>? {
        if (annotation.toAnnotationClassId(session) != localityAnnotationId) return null

        return LocalityAttribute()
    }

    override fun convertAttributeToAnnotation(attribute: ConeAttribute<*>): FirAnnotation? {
        return null
    }
}
