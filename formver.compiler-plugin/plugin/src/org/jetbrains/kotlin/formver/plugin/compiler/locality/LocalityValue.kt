package org.jetbrains.kotlin.formver.plugin.compiler.locality

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.formver.plugin.compiler.analysis.SymbolicValue

sealed interface LocalityValue : SymbolicValue<LocalityValue> {
    data object Global : LocalityValue

    data class Local(
        val owner: FirDeclaration? = null
    ) : LocalityValue

    override fun join(other: LocalityValue): LocalityValue =
        when (other) {
            Global -> this
            is Local -> other
        }

    override fun append(other: LocalityValue): LocalityValue = this

    fun accepts(other: LocalityValue): Boolean =
        when (this) {
            Global -> other == Global
            is Local -> when (other) {
                Global -> true
                is Local -> owner == other.owner
            }
        }

    fun render(): String =
        when (this) {
            Global -> "global"
            is Local -> "local(${(owner?.symbol as? FirCallableSymbol<*>)?.name ?: "unknown"})"
        }
}
