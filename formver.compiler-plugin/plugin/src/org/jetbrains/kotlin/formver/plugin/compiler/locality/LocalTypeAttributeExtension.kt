package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.extensions.FirTypeAttributeExtension
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class LocalTypeAttributeExtension(
    session: FirSession,
    private val config: PluginConfiguration
) : FirTypeAttributeExtension(session) {
    companion object {
        private val borrowedAnnotationId = ClassId(
            FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")),
            Name.identifier("Borrowed")
        )

        fun getFactory(config: PluginConfiguration): Factory {
            return Factory { session -> LocalTypeAttributeExtension(session, config) }
        }
    }

    override fun extractAttributeFromAnnotation(annotation: FirAnnotation): ConeAttribute<*>? {
        if (!config.checkLocality) return null

        return if (annotation.annotationTypeRef.coneType.classId == borrowedAnnotationId) {
            ConeLocalAttribute(null)
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