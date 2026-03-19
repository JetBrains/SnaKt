package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.extensions.FirTypeAttributeExtension
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.plugin.compiler.locality.ConeLocalAttribute
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Extension attaching the [ConeLocalAttribute] to types annotated with `@Borrowed`.
 */
class LocalTypeAttributeExtension(
    session: FirSession,
    private val config: PluginConfiguration
) : FirTypeAttributeExtension(session) {
    private val borrowedAnnotationId = ClassId(
        FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")),
        Name.identifier("Borrowed")
    )

    override fun extractAttributeFromAnnotation(annotation: FirAnnotation): ConeAttribute<*>? {
        if (!config.checkUniqueness) return null

        return if (annotation.annotationTypeRef.coneType.classId == borrowedAnnotationId) {
            ConeLocalAttribute
        } else {
            null
        }
    }

    override fun convertAttributeToAnnotation(attribute: ConeAttribute<*>): FirAnnotation? {
        if (attribute !is ConeLocalAttribute) return null

        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = borrowedAnnotationId.toLookupTag().constructClassType()
            }
        }
    }
}
