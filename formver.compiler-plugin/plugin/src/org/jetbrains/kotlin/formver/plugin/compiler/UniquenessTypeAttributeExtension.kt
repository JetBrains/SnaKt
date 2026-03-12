package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirTypeAttributeExtension
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.uniqueness.BorrowLevel
import org.jetbrains.kotlin.formver.uniqueness.UniqueLevel
import org.jetbrains.kotlin.formver.uniqueness.UniquenessConeAttribute
import org.jetbrains.kotlin.formver.uniqueness.UniquenessType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class UniquenessTypeAttributeExtension(session: FirSession, private val config: PluginConfiguration) :

    FirTypeAttributeExtension(session) {

    companion object {
        private fun getAnnotationId(name: String): ClassId =
            ClassId(
                FqName.Companion.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")),
                Name.identifier(name)
            )

        private val uniqueId: ClassId
            get() = getAnnotationId("Unique")

        private val borrowId: ClassId
            get() = getAnnotationId("Borrowed")
    }

    override fun extractAttributeFromAnnotation(annotation: FirAnnotation): ConeAttribute<*>? {
        if (!config.checkUniqueness) return null
        val annotationId = annotation.annotationTypeRef.coneType.classId ?: return null

        when (annotationId) {
            uniqueId -> {
                return UniquenessConeAttribute(
                    UniquenessType.Active(
                        UniqueLevel.Unique,
                        BorrowLevel.Global
                    )
                )
            }
            borrowId -> {
                return UniquenessConeAttribute(
                    UniquenessType.Active(
                        UniqueLevel.Shared,
                        BorrowLevel.Local
                    )
                )
            }
            else -> {
                return null
            }
        }
    }

    override fun convertAttributeToAnnotation(attribute: ConeAttribute<*>): FirAnnotation? {
        if (attribute !is UniquenessConeAttribute) return null
        val annotationClassId = when (attribute.type) {
            UniquenessType.Active(UniqueLevel.Unique, BorrowLevel.Global) -> uniqueId
            UniquenessType.Active(UniqueLevel.Shared, BorrowLevel.Local) -> borrowId
            else -> return null
        }

        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    annotationClassId.toLookupTag(),
                    emptyArray(),
                    false
                )
            }
            argumentMapping = FirEmptyAnnotationArgumentMapping
        }
    }

}