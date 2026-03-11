/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

/**
 * Determines the Viper permission management strategy for a field or property.
 *
 * Viper uses a separation-logic permission model: reading or writing a heap location requires
 * holding an appropriate permission. This policy controls whether permissions are inserted
 * automatically by the plugin or managed manually by the programmer.
 */
enum class AccessPolicy {
    /**
     * Permission is automatically inhaled (granted) before each field access and exhaled (consumed)
     * after. Use this for class fields accessed through class predicates, where the permission is
     * bundled in the predicate and must be temporarily extracted for each access.
     */
    ALWAYS_INHALE_EXHALE,

    /**
     * The field is always readable; the permission is held persistently and does not need to be
     * inhaled/exhaled around each access. Typically used for immutable or globally-accessible fields.
     */
    ALWAYS_READABLE,

    /**
     * The field is always writeable; the permission is held persistently. Typically used for
     * mutable fields where write access is continuously held by the enclosing method.
     */
    ALWAYS_WRITEABLE,

    /**
     * The programmer manages access permissions manually via the `@Manual` annotation.
     * No automatic inhale/exhale statements are inserted for fields with this policy.
     * @see org.jetbrains.kotlin.formver.plugin.Manual
     */
    MANUAL;
}
