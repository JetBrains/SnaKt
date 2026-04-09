package org.jetbrains.kotlin.formver.plugin.compiler.uniqueness

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

object LiteralSymbol : FirBasedSymbol<FirDeclaration>()