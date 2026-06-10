package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.formver.type.plugin.TypeUnifier

enum class Access {
    Intermediate, Terminal
}

fun Access.join(other: Access): Access =
    maxOf(this, other)

object AccessUnifier : TypeUnifier<Access> {
    override fun join(left: Access, right: Access): Access {
        return left.join(right)
    }
}
