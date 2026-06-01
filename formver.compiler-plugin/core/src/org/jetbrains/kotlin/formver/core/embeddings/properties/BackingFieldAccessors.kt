/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.expression.*

class BackingFieldGetter(val field: FieldEmbedding) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, receiverIsUnique: Boolean, ctx: TypeResolver): ExpEmbedding {
        return when (field.accessPolicy) {
            AccessPolicy.BY_RECEIVER_UNIQUENESS -> FieldAccess(receiver, receiverIsUnique, field)
            else -> FieldAccess(receiver, true, field).withInvariants(ctx) {
                proven = true
                access = true
            }
        }
    }

    override fun getValueSimple(
        receiver: ExpEmbedding,
        ctx: TypeResolver
    ): ExpEmbedding = FieldAccess(receiver, true, field)
}

class BackingFieldSetter(val field: FieldEmbedding) : SetterEmbedding {
    override fun setValue(
        receiver: ExpEmbedding,
        receiverIsUnique: Boolean,
        value: ExpEmbedding,
        ctx: StmtConversionContext
    ): ExpEmbedding =
        FieldModification(receiver, receiverIsUnique, field, value.withType(field.type))
}
