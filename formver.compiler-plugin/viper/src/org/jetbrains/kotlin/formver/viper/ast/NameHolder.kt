/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.AnyName

/**
 * A node that may reference [AnyName]s and contains child nodes to recurse into.
 *
 * Implementations describe their own contribution to the resolver-registration
 * walk: [directlyReferencedNames] for names that appear in this node, and
 * [children] for sub-nodes whose names must also be registered. Adding a new
 * Exp/Stmt variant requires implementing these accessors locally; there is no
 * central walker to forget to extend.
 */
interface NameHolder {
    val directlyReferencedNames: List<AnyName> get() = emptyList()
    val children: List<NameHolder> get() = emptyList()
}
