/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

/**
 * The root of the Viper AST: a complete, self-contained Viper program ready for verification.
 *
 * Mirrors `viper.silver.ast.Program` exactly; each field corresponds to a top-level Viper
 * declaration category:
 * - [domains]: algebraic data-type definitions used for runtime type encoding.
 * - [fields]: heap fields shared across all objects.
 * - [functions]: pure (side-effect-free) Viper functions, used in specifications.
 * - [predicates]: recursive permission abstractions (shared/unique access predicates).
 * - [methods]: impure Viper methods with pre/postconditions and statement bodies.
 *
 * [pos], [info], and [trafos] carry source-position, annotation, and transformation metadata
 * required by the Silver AST but left at their defaults in normal usage.
 */
data class Program(
    val domains: List<Domain>,
    val fields: List<Field>,
    val functions: List<Function>,
    val predicates: List<Predicate>,
    val methods: List<Method>,
    /* no extensions */
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Program> {

    /**
     * Converts this Kotlin-side Viper AST into the Silver (`viper.silver.ast`) representation
     * consumed directly by Silicon for verification.
     *
     * Each list is sorted by mangled name before conversion so that the Silver program's
     * declaration order is deterministic regardless of the order embeddings were registered.
     * A [NameResolver] context is required so that symbolic names can be resolved to strings.
     */
    context(nameResolver: NameResolver)
    override fun toSilver(): viper.silver.ast.Program = viper.silver.ast.Program(
        domains.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        fields.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        functions.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        predicates.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        methods.sortedBy { it.name.mangled }.toSilver().toScalaSeq(),
        emptySeq(), /* extensions */
        pos.toSilver(),
        info.toSilver(),
        trafos.toSilver(),
    )

    /**
     * Returns a filtered copy of this program suitable for short diagnostic dumps.
     *
     * Excludes declarations marked as [IncludeInDumpPolicy.ONLY_IN_FULL_DUMP] (e.g. internal
     * boilerplate predicates and functions) so that log output shown to the user is concise.
     * Used by `ViperPoweredDeclarationChecker` when the log level is `SHORT_VIPER_DUMP`.
     */
    fun toShort(): Program = Program(
        domains.filter { it.includeInShortDump },
        fields.filter { it.includeInShortDump },
        functions.filter { it.includeInDumpPolicy != IncludeInDumpPolicy.ONLY_IN_FULL_DUMP },
        predicates.filter { it.includeInDumpPolicy != IncludeInDumpPolicy.ONLY_IN_FULL_DUMP },
        methods.filter { it.includeInShortDump },
        pos,
        info,
        trafos,
    )

    /**
     * Returns a copy of this program with predicates and functions stripped down to those
     * marked [IncludeInDumpPolicy.ALWAYS].
     *
     * Combined with [toShort] for the `SHORT_VIPER_DUMP` log level to hide predicate noise,
     * making the output focus on user-visible methods and specifications.
     */
    fun withoutPredicates(): Program = copy(
        predicates = predicates.filter { it.includeInDumpPolicy == IncludeInDumpPolicy.ALWAYS },
        functions = functions.filter { it.includeInDumpPolicy == IncludeInDumpPolicy.ALWAYS }
    )

    /**
     * Converts this program to Silver and returns its string representation.
     * Used to emit the `VIPER_TEXT` diagnostic for logging/debugging.
     */
    context(nameResolver: NameResolver)
    fun toDebugOutput(): String = toSilver().toString()
}

/**
 * Recursively registers all symbolic names appearing in a Viper expression [exp]
 * with the [NameResolver], so they can be resolved to human-readable strings in debug output.
 *
 * Handles the expression node types that carry names:
 * - [Exp.LocalVar]: registers the variable name.
 * - [Exp.FieldAccess]: registers the field name and recurses into the receiver.
 * - [Exp.PredicateAccess]: registers the predicate name and recurses into arguments.
 * - [BinaryExp]: recurses into both operands.
 * - [Exp.FuncApp]: registers the function name and recurses into arguments.
 * - [Exp.DomainFuncApp]: registers the domain function name, its formal argument names,
 *   and recurses into the call-site arguments.
 * - All other node types are silently ignored (they carry no resolvable names).
 */
context(nameResolver: NameResolver)
private fun registerExpNames(exp: Exp) {
    when (exp) {
        is Exp.LocalVar -> nameResolver.register(exp.name)
        is Exp.FieldAccess -> {
            nameResolver.register(exp.field.name)
            registerExpNames(exp.rcv)
        }
        is Exp.PredicateAccess -> {
            nameResolver.register(exp.predicateName)
            exp.formalArgs.forEach { registerExpNames(it) }
        }
        is BinaryExp -> {
            registerExpNames(exp.left)
            registerExpNames(exp.right)
        }
        is Exp.FuncApp -> {
            nameResolver.register(exp.functionName)
            exp.args.forEach { registerExpNames(it) }
        }
        is Exp.DomainFuncApp -> {
            nameResolver.register(exp.function.name)
            exp.function.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
            exp.args.forEach { registerExpNames(it) }
        }
        else -> { }
    }
}

/**
 * Recursively registers all symbolic names appearing in a statement block [seqn]
 * with the [NameResolver].
 *
 * Walks the scoped declarations (local variables, labels) and every statement in the block,
 * registering names for:
 * - [Declaration.LocalVarDecl] and [Declaration.LabelDecl] in scope declarations.
 * - [Stmt.Label], [Stmt.Goto], [Stmt.MethodCall], [Stmt.LocalVarAssign],
 *   [Stmt.Fold], [Stmt.Unfold] in the statement list.
 * - Nested [Stmt.Seqn], [Stmt.If], and [Stmt.While] bodies are recursed into.
 */
context(nameResolver: NameResolver)
private fun Program.registerSeqnNames(seqn: Stmt.Seqn) {
    seqn.scopedSeqnDeclarations.forEach { decl ->
        when (decl) {
            is Declaration.LocalVarDecl -> nameResolver.register(decl.name)
            is Declaration.LabelDecl -> nameResolver.register(decl.name)
        }
    }
    seqn.stmts.forEach { stmt ->
        when (stmt) {
            is Stmt.Label -> nameResolver.register(stmt.name)
            is Stmt.Seqn -> registerSeqnNames(stmt)
            is Stmt.Goto -> nameResolver.register(stmt.name)
            is Stmt.MethodCall -> nameResolver.register(stmt.methodName)
            is Stmt.If -> {
                registerSeqnNames(stmt.then)
                stmt.els?.let { registerSeqnNames(it) }
            }
            is Stmt.While -> registerSeqnNames(stmt.body)
            is Stmt.LocalVarAssign -> nameResolver.register(stmt.lhs.name)
            is Stmt.Fold -> nameResolver.register(stmt.acc.predicateName)
            is Stmt.Unfold -> nameResolver.register(stmt.acc.predicateName)
            else -> { }
        }
    }
}

/**
 * Registers every symbolic name in the entire [Program] with the [NameResolver].
 *
 * Called once after conversion and before logging or verification, so that all names in the
 * program are resolvable for debug output and error messages. Covers:
 * - **Domains**: domain names, domain function names, and their formal argument names.
 * - **Fields**: field names.
 * - **Functions**: function names, formal argument names, and all names in the body expression.
 * - **Predicates**: predicate names, formal argument names, and all names in the body expression.
 * - **Methods**: method names, formal argument names, return variable names, and all names
 *   in the body statement block (via [registerSeqnNames]).
 */
context(nameResolver: NameResolver)
fun Program.registerAllNames() {
    domains.forEach { domain ->
        nameResolver.register(domain.name)
        domain.functions.forEach { function ->
            nameResolver.register(function.name)
            function.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        }
    }
    fields.forEach { nameResolver.register(it.name) }

    functions.forEach { function ->
        nameResolver.register(function.name)
        function.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        function.body?.let { exp -> registerExpNames(exp) }
    }

    predicates.forEach { predicate ->
        nameResolver.register(predicate.name)
        predicate.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        registerExpNames(predicate.body)
    }

    methods.forEach { method ->
        nameResolver.register(method.name)
        method.formalArgs.forEach { arg -> nameResolver.register(arg.name) }
        method.formalReturns.forEach { ret -> nameResolver.register(ret.name) }
        method.body?.let { seqn -> registerSeqnNames(seqn) }
    }
}