/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.*

class BackingFieldGetter(val field: FieldEmbedding) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding {
        return when (field.accessPolicy) {
            AccessPolicy.ALWAYS_READABLE, AccessPolicy.BY_RECEIVER_UNIQUENESS -> FieldAccess(receiver, field)
            else -> FieldAccess(receiver, field).withInvariants(ctx.typeResolver) {
                proven = true
                access = true
            }
        }
    }
}

class BackingFieldSetter(val field: FieldEmbedding) : SetterEmbedding {
    override fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding =
        FieldModification(receiver, field, value.withType(field.type))
}
