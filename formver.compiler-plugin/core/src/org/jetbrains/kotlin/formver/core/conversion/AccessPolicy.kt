/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

enum class AccessPolicy {
    /** var fields **/
    BY_RECEIVER_UNIQUENESS,

    /** val fields **/
    ALWAYS_READABLE,

    /** special fields (like the length of a list) **/
    ALWAYS_WRITEABLE,

    /** with @Manual annotated fields **/
    MANUAL;
}
