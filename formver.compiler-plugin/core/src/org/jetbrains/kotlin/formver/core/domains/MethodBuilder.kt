/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.domains

import org.jetbrains.kotlin.formver.core.names.PlaceholderArgumentName
import org.jetbrains.kotlin.formver.core.names.PlaceholderReturnVariableName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.BuiltInMethod
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Type
import org.jetbrains.kotlin.formver.viper.ast.Var

/**
 * DSL language for construction of Viper methods.
 * Should be used via `build` method of companion object.
 *
 * Currently, only abstract methods are supported.
 *
 * After `argument` clause arguments can be used via `args` List.
 * After `returns` clause result can be used via `result`.
 */
class MethodBuilder private constructor() {
    private val pres = mutableListOf<Exp>()
    private val posts = mutableListOf<Exp>()
    private val formalArgs = mutableListOf<Var>()
    private lateinit var retVar: Var

    val args: List<Exp.LocalVar> = object : AbstractList<Exp.LocalVar>() {
        override val size: Int
            get() = formalArgs.size

        override fun get(index: Int) = formalArgs[index].use()
    }

    val result
        get() = retVar

    companion object {
        fun build(name: SymbolicName, action: MethodBuilder.() -> Unit): BuiltInMethod {
            val builder = MethodBuilder()
            builder.action()
            return BuiltInMethod(
                name = name,
                formalArgs = builder.formalArgs.map { it.decl() },
                returnVar = builder.retVar.decl(),
                body = null,
                pres = builder.pres,
                posts = builder.posts
            )
        }
    }

    fun precondition(action: () -> Exp): Exp {
        val exp = action()
        pres.add(exp)
        return exp
    }

    fun precondition(exp: Exp) = precondition { exp }

    fun postcondition(action: () -> Exp): Exp {
        val exp = action()
        posts.add(exp)
        return exp
    }

    fun postcondition(exp: Exp) = postcondition { exp }

    fun argument(action: () -> Type): Exp.LocalVar {
        val argType = action()
        val variable = Var(PlaceholderArgumentName(formalArgs.size + 1), argType)
        formalArgs.add(variable)
        return variable.use()
    }

    fun argument(type: Type) = argument { type }

    fun returns(action: () -> Type): Exp.Result {
        val retType = action()
        val variable = Var(PlaceholderReturnVariableName, retType)
        retVar = variable
        return Exp.Result(retType)
    }

    fun returns(type: Type) = returns { type }

}
