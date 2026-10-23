/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.properties.BackingFieldGetter
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.buildType
import org.jetbrains.kotlin.formver.core.names.asScope
import org.jetbrains.kotlin.formver.core.names.embedName
import org.jetbrains.kotlin.formver.viper.ast.Type
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExternalMarkerTypesTest {
    private val serializableId = ClassId.topLevel(FqName("java.io.Serializable"))

    @Test
    fun serializableKeepsItsNominalIdentity() {
        val resolver = TypeResolver()
        val marker = assertNotNull(resolver.embedExternalMarker(serializableId))

        assertEquals(serializableId.embedName(), marker.name)
        assertSame(marker, resolver.embedExternalMarker(serializableId))
        assertSame(marker, resolver.lookupClassTypeEmbedding(marker.name))
        assertEquals(listOf(marker), resolver.classTypeEmbeddings())
        assertTrue(marker.accessInvariants(resolver).isEmpty())
    }

    @Test
    fun markerDoesNotReplaceOtherSupertypes() {
        val resolver = TypeResolver()
        val marker = assertNotNull(resolver.embedExternalMarker(serializableId))
        val parent = ClassTypeEmbedding(ClassId.topLevel(FqName("example.Parent")).embedName())
        val child = ClassTypeEmbedding(ClassId.topLevel(FqName("example.Child")).embedName())
        resolver.register(parent, isInterface = false)
        resolver.register(child, isInterface = false)
        // Insert the interface first: superclass traversal must still choose Parent.
        resolver.addSubtypeRelation(child.name, marker.name)
        resolver.addSubtypeRelation(child.name, parent.name)
        val field = object : FieldEmbedding {
            override val name = parent.name
            override val type = buildType { int() }
            override val viperType = Type.Ref
            override val accessPolicy = AccessPolicy.ALWAYS_WRITEABLE
            override val containingClass = parent
            override val includeInShortDump = true
        }
        val key = ClassPropertyPair(parent.name, PropertyKotlinName(parent.name.asScope(), Name.identifier("value")))
        resolver.getOrPutProperty(key) {
            PropertyEmbedding(BackingFieldGetter(field), null, true, false, true, field.type)
        }

        assertEquals(listOf(marker, parent), resolver.lookupSuperTypes(child.name))
        assertEquals(listOf(child, parent), resolver.hierarchyPathTo(child, field).toList())
        assertEquals(listOf(field), resolver.flatMapUniqueFields(child.name) { listOf(it) })
        assertEquals(parent.accessInvariants(resolver), child.accessInvariants(resolver))
    }

    @Test
    fun otherExternalTypesAreNotAssumedToBeEmptyMarkers() {
        val resolver = TypeResolver()
        for (name in listOf("example.Serializable", "java.lang.Runnable", "java.io.Externalizable")) {
            assertNull(resolver.embedExternalMarker(ClassId.topLevel(FqName(name))))
        }
        assertTrue(resolver.classTypeEmbeddings().isEmpty())
    }
}
