package org.jetbrains.kotlin.formver.plugin.compiler.analysis

interface SymbolicValue<T : SymbolicValue<T>> {
    fun join(other: T): T

    fun append(other: T): T
}
