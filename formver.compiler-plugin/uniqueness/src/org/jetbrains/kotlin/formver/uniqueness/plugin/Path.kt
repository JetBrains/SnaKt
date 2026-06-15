package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.declarations.utils.memberDeclarationNameOrNull
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import kotlin.collections.joinToString

typealias Path = List<FirBasedSymbol<*>>

val PathRenderer = Renderer<Path> { path ->
    path.map({symbol -> symbol.memberDeclarationNameOrNull}).joinToString(".")
}
