/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType

fun FirTypeRef.resolveLocalityRequirement(): LocalityRequirement {
    if (coneType.attributes.locality == null) return LocalityRequirement.RequireGlobal

    return LocalityRequirement.RequireLocal
}
