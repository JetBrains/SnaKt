/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.embedName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val serializableId = ClassId.topLevel(FqName("java.io.Serializable"))

/**
 * Serializable has no instance members or superinterfaces. Its serialization protocol is not
 * modelled here: this embedding preserves only its nominal type identity, with no owned fields.
 * Registration as an interface keeps the normal subtype and inherited-resource machinery intact.
 * Other external types must go through ordinary type conversion, not an empty-marker fallback.
 */
fun TypeResolver.embedExternalMarker(classId: ClassId): ClassTypeEmbedding? {
    if (classId != serializableId) return null
    val name = classId.embedName()
    lookupClassTypeEmbedding(name)?.let { return it }
    return ClassTypeEmbedding(name).also { register(it, isInterface = true) }
}
