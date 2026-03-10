/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.core.embeddings.LabelLink
import org.jetbrains.kotlin.formver.core.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.insertCall
import org.jetbrains.kotlin.formver.core.embeddings.callables.isVerifyFunction
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GeCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GtCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GtIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LeCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LtCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LtIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Not
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.equalToType
import org.jetbrains.kotlin.formver.core.functionCallArguments
import org.jetbrains.kotlin.formver.core.isInvariantBuilderFunctionNamed
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Convert a statement, emitting the resulting Viper statements and
 * declarations into the context, returning a reference to the
 * expression containing the result. Note that in the FIR, expressions
 * are a subtype of statements.
 *
 * In many cases, we introduce a temporary variable to represent this
 * result (since, for example, a method call is not an expression).
 * When the result is an lvalue, it is important to return an expression
 * that refers to location, not just the same value, and so introducing
 * a temporary variable for the result is not acceptable in those cases.
 *
 * This is a singleton `FirVisitor` that maps every FIR node type to an [ExpEmbedding].
 * It never produces Viper AST directly — that happens later in the `Linearizer` /
 * `PureLinearizer` stage. The [StmtConversionContext] passed as `data` carries all mutable
 * state (scope, loop index, when-subject, etc.) needed during traversal.
 *
 * Unsupported FIR nodes are handled by [handleUnimplementedElement], which either throws
 * or records a minor error depending on [UnsupportedFeatureBehaviour].
 */
object StmtConversionVisitor : FirVisitor<ExpEmbedding, StmtConversionContext>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    /** Fallback for any FIR node not explicitly handled; delegates to [handleUnimplementedElement]. */
    override fun visitElement(element: FirElement, data: StmtConversionContext): ExpEmbedding =
        handleUnimplementedElement(element.source, "Not yet implemented for $element (${element.source.text})", data)

    /**
     * Converts a `return` expression.
     * - A `return Unit` / implicit return maps to [UnitLit].
     * - All other return values are converted recursively.
     * The resolved [ReturnTarget] is looked up by label name (null for an unlabelled return).
     */
    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val expr = when (returnExpression.result) {
            is FirUnitExpression -> UnitLit
            else -> data.convert(returnExpression.result)
        }
        // returnTarget is null when it is the implicit return of a lambda
        val returnTargetName = returnExpression.target.labelName
        val target = data.resolveReturnTarget(returnTargetName)
        return Return(expr, target)
    }

    /**
     * Converts a resolved qualifier (e.g. a companion object reference used as a statement).
     * Only `Unit`-typed qualifiers are currently supported; anything else is a hard error.
     */
    override fun visitResolvedQualifier(
        resolvedQualifier: FirResolvedQualifier, data: StmtConversionContext
    ): ExpEmbedding {
        if (!resolvedQualifier.resolvedType.isUnit) {
            throw SnaktInternalException(
                resolvedQualifier.source, "Only `Unit` is supported among resolved qualifiers currently."
            )
        }
        return UnitLit
    }

    /**
     * Converts a block by converting each statement in order and collecting the results
     * into a single [Block] embedding.
     */
    override fun visitBlock(block: FirBlock, data: StmtConversionContext): ExpEmbedding =
        block.statements.map(data::convert).toBlock()

    /**
     * Converts a literal constant expression to the corresponding embedding:
     * - `Int` → [IntLit]
     * - `Boolean` → [BooleanLit]
     * - `Char` → [CharLit]
     * - `String` → [StringLit]
     * - `null` → [NullLit]
     * - Other literal kinds → [handleUnimplementedElement]
     */
    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: StmtConversionContext,
    ): ExpEmbedding = when (literalExpression.kind) {
        ConstantValueKind.Int -> IntLit((literalExpression.value as Long).toInt())
        ConstantValueKind.Boolean -> BooleanLit(literalExpression.value as Boolean)
        ConstantValueKind.Char -> CharLit(literalExpression.value as Char)
        ConstantValueKind.String -> StringLit(literalExpression.value as String)
        ConstantValueKind.Null -> NullLit
        else -> handleUnimplementedElement(
            literalExpression.source,
            "Constant Expression of type ${literalExpression.kind} is not yet implemented.",
            data
        )
    }

    /** Returns the string representation of this literal's value. */
    private val FirLiteralExpression.stringValue: String
        get() = value.toString()

    /**
     * Converts a string concatenation by joining all literal arguments into a single [StringLit].
     * Non-literal arguments (e.g. string templates containing expressions) are not yet supported.
     */
    override fun visitStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall, data: StmtConversionContext
    ): ExpEmbedding {
        val combinedLiteral = stringConcatenationCall.arguments.joinToString("") { arg ->
            if (arg !is FirLiteralExpression) {
                throw SnaktInternalException(
                    arg.source, "${arg::class.simpleName} is not supported as an element of string concatenation."
                )
            }
            arg.stringValue
        }
        return StringLit(combinedLiteral)
    }

    /**
     * Integer literal operator calls (e.g. `1 + 2` where the literal types are resolved at the
     * call site) are handled identically to regular function calls.
     */
    override fun visitIntegerLiteralOperatorCall(
        integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        return visitFunctionCall(integerLiteralOperatorCall, data)
    }

    /**
     * Returns the pre-stored [StmtConversionContext.whenSubject] variable for a `when` subject reference.
     * The subject is set by [visitWhenExpression] before branches are converted.
     */
    override fun visitWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: StmtConversionContext,
    ): ExpEmbedding = data.whenSubject!!

    /**
     * Recursively converts the branches of a `when` expression into nested [If] embeddings.
     * The last branch is either an `else` (mapped to its body directly) or a regular
     * condition branch (mapped to `If(cond, then, convertRemainingBranches(...))`).
     * Returns [UnitLit] when no branches remain.
     */
    private fun convertWhenBranches(
        whenBranches: Iterator<FirWhenBranch>,
        type: TypeEmbedding,
        data: StmtConversionContext,
    ): ExpEmbedding {
        if (!whenBranches.hasNext()) return UnitLit

        val branch = whenBranches.next()

        // Note that only the last condition can be a FirElseIfTrue
        return if (branch.condition is FirElseIfTrueCondition) {
            data.withNewScope { convert(branch.result) }
        } else {
            val cond = data.convert(branch.condition).withType { boolean() }
            val thenExp = data.withNewScope { convert(branch.result) }
            val elseExp = convertWhenBranches(whenBranches, type, data)
            If(cond, thenExp, elseExp, type)
        }
    }

    /**
     * Converts a `when` expression:
     * 1. If the `when` has a subject variable, it is declared as a local (or anon) variable.
     * 2. The subject is set as [StmtConversionContext.whenSubject] for branch conversion.
     * 3. Branches are converted into nested [If] embeddings via [convertWhenBranches].
     * 4. The subject declaration (if any) is prepended to the result in a [blockOf].
     */
    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext): ExpEmbedding =
        data.withNewScope {
            val type = data.embedType(whenExpression)
            val subj: Declare? = whenExpression.subjectVariable?.let { firSubjVar ->
                val subjExp = convert(firSubjVar.initializer!!)
                if (firSubjVar.name.isSpecial)
                    declareAnonVar(subjExp.type, subjExp)
                else
                    declareLocalVariable(firSubjVar.symbol, subjExp)
            }
            val body = withWhenSubject(subj?.variable) {
                convertWhenBranches(whenExpression.branches.iterator(), type, this)
            }
            subj?.let { blockOf(it, body) } ?: body
        }

    /**
     * Converts a property read (`receiver.field` or a local variable reference).
     * Delegates to [embedPropertyAccess] for the access embedding, then calls `getValue`
     * to produce the read expression.
     */
    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val propertyAccess = data.embedPropertyAccess(propertyAccessExpression)
        return propertyAccess.getValue(data)
    }

    /**
     * Converts an equality comparison (`==` or `!=`).
     * Both operands are converted and compared with [EqCmp]; `!=` wraps the result in [Not].
     * Any other operation (e.g. identity `===`) is currently unsupported.
     */
    override fun visitEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        if (equalityOperatorCall.arguments.size != 2) {
            throw SnaktInternalException(
                equalityOperatorCall.source,
                "Invalid equality comparison $equalityOperatorCall, can only compare 2 elements."
            )
        }
        val left = data.convert(equalityOperatorCall.arguments[0])
        val right = data.convert(equalityOperatorCall.arguments[1])

        return when (equalityOperatorCall.operation) {
            FirOperation.EQ -> convertEqCmp(left, right)
            FirOperation.NOT_EQ -> Not(convertEqCmp(left, right))
            else -> handleUnimplementedElement(
                equalityOperatorCall.source,
                "Equality comparison operation ${equalityOperatorCall.operation} not yet implemented.",
                data
            )
        }
    }

    /**
     * Wraps two embeddings in an [EqCmp].
     * TODO: replace with a call to `left.equals()` for proper structural equality.
     */
    private fun convertEqCmp(left: ExpEmbedding, right: ExpEmbedding): ExpEmbedding {
        //TODO: replace with call to left.equals()
        return EqCmp(left, right)
    }

    /**
     * Converts an ordering comparison (`<`, `<=`, `>`, `>=`).
     *
     * Resolves the `compareTo` callee type to pick the right operator template:
     * - Int operands → [IntComparisonExpEmbeddingsTemplate]
     * - Char operands → [CharComparisonExpEmbeddingsTemplate]
     * - Other types → falls back to converting the `compareTo` call and comparing the
     *   resulting Int against 0 using the Int template.
     */
    override fun visitComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {

        val dispatchReceiver: FirExpression =
            comparisonExpression.compareToCall.dispatchReceiver ?: throw SnaktInternalException(
                comparisonExpression.compareToCall.source, "found 'compareTo' call with null receiver"
            )
        val arg =
            comparisonExpression.compareToCall.argumentList.arguments.firstOrNull() ?: throw SnaktInternalException(
                comparisonExpression.compareToCall.source, "found `compareTo` call with no argument at position 0"
            )
        val left = data.convert(dispatchReceiver)
        val right = data.convert(arg)

        val functionSymbol = comparisonExpression.compareToCall.toResolvedCallableSymbol()

        val functionType = data.embedFunctionPretype(functionSymbol as FirFunctionSymbol)

        val comparisonTemplate = when {
            functionType.formalArgTypes.all { it.equalToType { int() } } -> IntComparisonExpEmbeddingsTemplate
            functionType.formalArgTypes.all { it.equalToType { char() } } -> CharComparisonExpEmbeddingsTemplate
            else -> {
                val result = data.convert(comparisonExpression.compareToCall)
                return IntComparisonExpEmbeddingsTemplate.retrieve(comparisonExpression.operation)(result, IntLit(0))
            }
        }
        return comparisonTemplate.retrieve(comparisonExpression.operation)(left, right)
    }

    /** Selects a [BinaryOperatorExpEmbeddingTemplate] for a given comparison [FirOperation]. */
    private interface ComparisonExpEmbeddingsTemplate {
        fun retrieve(operation: FirOperation): BinaryOperatorExpEmbeddingTemplate
    }

    /** Maps `<`, `<=`, `>`, `>=` to the corresponding Int Viper operator templates. */
    private object IntComparisonExpEmbeddingsTemplate : ComparisonExpEmbeddingsTemplate {
        override fun retrieve(operation: FirOperation) = when (operation) {
            FirOperation.LT -> LtIntInt
            FirOperation.LT_EQ -> LeIntInt
            FirOperation.GT -> GtIntInt
            FirOperation.GT_EQ -> GeIntInt
            else -> throw IllegalArgumentException("Expected comparison operation but found ${operation}.")
        }
    }

    /** Maps `<`, `<=`, `>`, `>=` to the corresponding Char Viper operator templates. */
    private object CharComparisonExpEmbeddingsTemplate : ComparisonExpEmbeddingsTemplate {
        override fun retrieve(operation: FirOperation) = when (operation) {
            FirOperation.LT -> LtCharChar
            FirOperation.LT_EQ -> LeCharChar
            FirOperation.GT -> GtCharChar
            FirOperation.GT_EQ -> GeCharChar
            else -> throw IllegalArgumentException("Expected comparison operation but found ${operation}.")
        }
    }

    /**
     * Expands a vararg argument list, converting each element individually.
     * Vararg arguments are only supported for the `verify()` function; any other use is a hard error.
     * Non-vararg arguments are converted normally via `data::convert`.
     */
    private fun List<FirExpression>.withVarargsHandled(data: StmtConversionContext, function: CallableEmbedding?) =
        flatMap { arg ->
            when (arg) {
                is FirVarargArgumentsExpression -> {
                    // Short circuit as isVerifyFunction property only exists on embedding of type FunctionEmbedding
                    if (function == null || function is FunctionEmbedding && !function.isVerifyFunction) {
                        throw SnaktInternalException(
                            arg.source, "Vararg arguments are currently supported for `verify` function only."
                        )
                    }
                    arg.arguments.map(data::convert)
                }

                else -> listOf(data.convert(arg))
            }
        }

    /**
     * Converts a function call.
     *
     * Two cases:
     * - **`forAll { }` call**: validated to be inside a specification block, then routed to
     *   [insertForAllFunctionCall] to produce a [ForAllEmbedding] quantifier.
     * - **Regular call**: the callee is embedded via `embedAnyFunction` (pure or impure),
     *   arguments are converted and varargs are expanded, then `insertCall` produces the
     *   appropriate [FunctionCall] or [MethodCall] embedding.
     */
    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): ExpEmbedding {
        val symbol = functionCall.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: throw NotImplementedError("Only functions are expected as callables of function calls, got ${functionCall.toResolvedCallableSymbol()}")

        when (val forAllLambda = functionCall.extractFormverFirBlock { isInvariantBuilderFunctionNamed("forAll") }) {
            null -> {
                val callee = data.embedAnyFunction(symbol)
                return callee.insertCall(
                    functionCall.functionCallArguments.withVarargsHandled(data, callee),
                    data,
                    data.embedType(functionCall.resolvedType),
                )
            }

            else -> {
                if (!data.isValidForForAllBlock) throw SnaktInternalException(
                    forAllLambda.source,
                    "`forAll` scope is only allowed inside one of the `loopInvariants`, `preconditions` or `postconditions`."
                )
                //error("`forAll` scope is only allowed inside one of the `loopInvariants`, `preconditions` or `postconditions`.")
                val forAllArg = forAllLambda.valueParameters.first()
                val forAllBody = forAllLambda.body ?: throw SnaktInternalException(
                    forAllLambda.body?.source, "Lambda body should be accessible in `forAll` function call."
                )
                return data.insertForAllFunctionCall(forAllArg.symbol, forAllBody)
            }
        }
    }

    /**
     * Converts an implicit `invoke` call (calling a lambda or function object via `()`).
     *
     * Two cases:
     * - The receiver resolves to a [LambdaExp] local variable: the lambda is inlined directly
     *   via `insertCall`.
     * - Otherwise: wrapped in [InvokeFunctionObject] which calls the generic `invoke` method on
     *   the function object.
     */
    override fun visitImplicitInvokeCall(
        implicitInvokeCall: FirImplicitInvokeCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val receiver =
            implicitInvokeCall.dispatchReceiver as? FirPropertyAccessExpression ?: throw SnaktInternalException(
                implicitInvokeCall.source,
                "Implicit invoke calls only support a limited range of receivers at the moment."
            )
        val returnType = data.embedType(implicitInvokeCall.resolvedType)
        val receiverSymbol = receiver.calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()!!
        val args = implicitInvokeCall.argumentList.arguments.withVarargsHandled(data, function = null)
        return when (val exp = data.embedLocalSymbol(receiverSymbol).ignoringMetaNodes()) {
            is LambdaExp -> {
                // The lambda is already the receiver, so we do not need to convert it.
                // TODO: do this more uniformly: convert the receiver, see it is a lambda, use insertCall on it.
                exp.insertCall(args, data, returnType)
            }

            else -> {
                InvokeFunctionObject(data.convert(receiver), args, returnType)
            }
        }
    }

    /**
     * Converts a local property declaration (`val` / `var`).
     * Non-local properties should not reach this visitor; they are a hard error.
     * Registers the property and emits a [Declare] with the converted initializer (if any),
     * with its type narrowed to the declared type.
     */
    override fun visitProperty(property: FirProperty, data: StmtConversionContext): ExpEmbedding {
        val symbol = property.symbol
        if (!symbol.isLocal) {
            throw SnaktInternalException(
                property.source,
                "StmtConversionVisitor should not encounter non-local properties."
            )
        }

        val type = data.embedType(symbol.resolvedReturnType)
        return data.declareLocalProperty(symbol, property.initializer?.let { data.convert(it).withType(type) })
    }

    /**
     * Converts a `while` loop.
     *
     * 1. Converts the loop condition.
     * 2. Collects loop invariants from all in-scope variables (shared predicate access and
     *    proven invariants) plus any explicit `loopInvariants { }` block in the body.
     * 3. Converts the loop body in a fresh while-loop scope (which assigns a new index for
     *    `break`/`continue` labels).
     * 4. Returns a [While] embedding with the condition, body, break/continue labels, and invariants.
     */
    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext): ExpEmbedding {
        val condition = data.convert(whileLoop.condition).withType { boolean() }
        val invariants = buildList {
            data.retrievePropertiesAndParameters().forEach {
                addIfNotNull(it.sharedPredicateAccessInvariant())
                addAll(it.provenInvariants())
            }
            extractLoopInvariants(whileLoop.block)?.let {
                addAll(data.withScopeImpl(ScopeIndex.NoScope) { data.collectInvariants(it) })
            }
        }
        return data.withFreshWhile(whileLoop.label) {
            val body = convert(whileLoop.block)
            While(condition, body, breakLabelName(), continueLabelName(), invariants)
        }
    }

    /**
     * Converts a `break` expression to a [Goto] targeting the break label of the
     * innermost (or named) while loop.
     */
    override fun visitBreakExpression(
        breakExpression: FirBreakExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val targetName = breakExpression.target.labelName
        val breakLabel = LabelLink(data.breakLabelName(targetName))
        return Goto(breakLabel)
    }

    /**
     * Converts a `continue` expression to a [Goto] targeting the continue label of the
     * innermost (or named) while loop.
     */
    override fun visitContinueExpression(
        continueExpression: FirContinueExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val targetName = continueExpression.target.labelName
        val continueLabel = LabelLink(data.continueLabelName(targetName))
        return Goto(continueLabel)
    }

    /**
     * Converts a desugared assignment value reference (the LHS placeholder in compound
     * assignments like `x += 1`). Delegates to converting the underlying expression.
     */
    override fun visitDesugaredAssignmentValueReferenceExpression(
        desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
        data: StmtConversionContext
    ): ExpEmbedding {
        return data.convert(desugaredAssignmentValueReferenceExpression.expressionRef.value)
    }

    /**
     * Converts a variable assignment (`x = expr` or `obj.field = expr`).
     *
     * The lvalue is embedded via [embedPropertyAccess] (handles local variables, backing fields,
     * and desugared compound assignments). The rvalue is converted, then `setValue` produces
     * the appropriate [Assign] or [FieldModification] embedding.
     */
    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val embedding = when (val lValue = variableAssignment.lValue) {
            is FirPropertyAccessExpression -> {
                data.embedPropertyAccess(lValue)
            }

            is FirDesugaredAssignmentValueReferenceExpression -> {
                data.embedPropertyAccess(lValue.expressionRef.value as FirPropertyAccessExpression)
            }

            else -> throw SnaktInternalException(
                variableAssignment.source, "Lvalue must be either property access or desugared assignment."
            )
        }
        val convertedRValue = data.convert(variableAssignment.rValue)
        return embedding.setValue(convertedRValue, data)
    }

    /**
     * Converts a smart-cast expression.
     *
     * If the cast is a simple null-narrowing (e.g. `T?` → `T`), the type is updated without
     * adding new invariants. Otherwise (e.g. `B` → `A`), access invariants for the target
     * type are inhaled to inform the verifier of the new type's permissions.
     */
    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val exp = data.convert(smartCastExpression.originalExpression)
        val newType = data.embedType(smartCastExpression.smartcastType.coneType)
        // If the smart-cast is from A? to A, then is not necessary to inhale invariants
        return if (exp.type.getNonNullable() == newType) {
            exp.withType(newType)
        } else {
            // TODO: when there is a cast from B to A, only inhale invariants of A - invariants of B
            exp.withNewTypeInvariants(newType) {
                access = true
            }
        }
    }

    /**
     * Converts a short-circuit boolean operator:
     * - `&&` → [SequentialAnd] (right operand only evaluated if left is true)
     * - `||` → [SequentialOr] (right operand only evaluated if left is false)
     */
    override fun visitBooleanOperatorExpression(
        booleanOperatorExpression: FirBooleanOperatorExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val left = data.convert(booleanOperatorExpression.leftOperand)
        val right = data.convert(booleanOperatorExpression.rightOperand)
        return when (booleanOperatorExpression.kind) {
            LogicOperationKind.AND -> SequentialAnd(left, right)
            LogicOperationKind.OR -> SequentialOr(left, right)
        }
    }

    /**
     * Converts a `this` receiver expression.
     *
     * Resolves the bound symbol to determine which receiver this refers to:
     * - [FirClassSymbol] → dispatch receiver (`this` in a member function).
     * - [FirAnonymousFunctionSymbol] → extension receiver of the anonymous function (lambda).
     * - [FirFunctionSymbol] → extension receiver of the named function.
     *
     * If the direct bound symbol doesn't resolve, falls back to the containing declaration symbol
     * (e.g. for [FirReceiverParameterSymbol] or [FirValueParameterSymbol]).
     */
    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        // `thisReceiverExpression` has a bound symbol which can be used for lookup
        // for extensions `this`es the bound symbol is the function they originate from
        // for member functions the bound symbol is a class they're defined in
        //
        // since dispatch receiver can only originate from non-anonymous function we do not specify its name here
        // as we have only one candidate to resolve it
        fun tryResolve(symbol: FirBasedSymbol<*>): ExpEmbedding? {
            val resolved = when (symbol) {
                is FirClassSymbol<*> -> data.resolveDispatchReceiver()
                is FirAnonymousFunctionSymbol -> data.resolveExtensionReceiver(symbol.label!!.name)
                is FirFunctionSymbol<*> -> data.resolveExtensionReceiver(symbol.name.asString())
                else -> return null
            }

            return resolved
                ?: throw SnaktInternalException(
                    thisReceiverExpression.source,
                    "Can't resolve the 'this' receiver since the function does not have one."
                )
        }

        val symbol = thisReceiverExpression.calleeReference.boundSymbol
        tryResolve(symbol as FirBasedSymbol<*>)?.let { return it }
        val declSymbol = when (symbol) {
            is FirReceiverParameterSymbol -> symbol.containingDeclarationSymbol
            is FirValueParameterSymbol -> symbol.containingDeclarationSymbol
            else -> throw SnaktInternalException(symbol.source, "Unsupported receiver expression type.")
        }
        tryResolve(declSymbol)?.let { return it }

        throw SnaktInternalException(thisReceiverExpression.source, "No resolution approach to this symbol worked.")
    }

    /**
     * Converts a type operator call (`is`, `!is`, `as`, `as?`):
     * - `is T` → [Is]
     * - `!is T` → `Not(Is(...))`
     * - `as T` → [Cast] with access + proven invariants inhaled.
     * - `as? T` → [SafeCast] with access + proven invariants inhaled (nullable result).
     * - Other operators → [handleUnimplementedElement].
     */
    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val argument = data.convert(typeOperatorCall.arguments[0])
        val conversionType = data.embedType(typeOperatorCall.conversionTypeRef.coneType)
        return when (typeOperatorCall.operation) {
            FirOperation.IS -> Is(argument, conversionType)
            FirOperation.NOT_IS -> Not(Is(argument, conversionType))
            FirOperation.AS -> Cast(argument, conversionType).withInvariants {
                proven = true
                access = true
            }

            FirOperation.SAFE_AS -> SafeCast(argument, conversionType).withInvariants {
                proven = true
                access = true
            }

            else -> handleUnimplementedElement(
                typeOperatorCall.source, "Can't embed type operator ${typeOperatorCall.operation}.", data
            )
        }
    }

    /**
     * Converts a lambda expression (anonymous function) into a [LambdaExp].
     * The lambda is **not** inlined here — inlining happens at the call site when the
     * lambda is passed to an inline function and [visitImplicitInvokeCall] is reached.
     */
    override fun visitAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val function = anonymousFunctionExpression.anonymousFunction
        return LambdaExp(data.embedFunctionSignature(function.symbol), function, data, function.symbol.label!!.name)
    }

    /**
     * Converts a `try`/`catch` expression.
     *
     * The try body is converted in a context where each catch clause has a registered entry
     * label. Non-deterministic [Goto]s to all catch entry labels are inserted at the start
     * and end of the try body to model the possibility of exceptions at any point.
     *
     * Each catch clause is then converted as a [GotoChainNode] that:
     * 1. Begins at its entry label.
     * 2. Declares the caught exception parameter (uninitialised — its value is unknown).
     * 3. Converts the catch body.
     * 4. Jumps to the shared exit label.
     *
     * The entire try-catch is assembled as a [Block] of the try body, all catch nodes,
     * and a final exit [LabelExp].
     */
    override fun visitTryExpression(tryExpression: FirTryExpression, data: StmtConversionContext): ExpEmbedding {
        val (catchData, tryBody) = data.withCatches(tryExpression.catches) { catchData ->
            withNewScope {
                val jumps =
                    catchData.blocks.map { catchBlock -> NonDeterministically(Goto(catchBlock.entryLabel.toLink())) }
                val body = convert(tryExpression.tryBlock)
                GotoChainNode(
                    null,
                    Block {
                        addAll(jumps)
                        add(body)
                        addAll(jumps)
                    },
                    catchData.exitLabel.toLink()
                )
            }
        }
        val catches = catchData.blocks.map { catchBlock ->
            data.withNewScope {
                val parameter = catchBlock.firCatch.parameter
                // The value is the thrown exception, which we do not know, hence we do not initialise the exception variable.
                val paramDecl = declareLocalProperty(parameter.symbol, null)
                GotoChainNode(
                    catchBlock.entryLabel,
                    blockOf(
                        paramDecl,
                        convert(catchBlock.firCatch.block)
                    ),
                    catchData.exitLabel.toLink()
                )
            }
        }
        return Block {
            add(tryBody)
            addAll(catches)
            add(LabelExp(catchData.exitLabel))
        }
    }

    /**
     * Converts an Elvis expression (`lhs ?: rhs`) into an [Elvis] embedding.
     * Both sides are converted; the result type is the non-nullable union of the two branches.
     */
    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val lhs = data.convert(elvisExpression.lhs)
        val rhs = data.convert(elvisExpression.rhs)
        val expType = data.embedType(elvisExpression.resolvedType)
        return Elvis(lhs, rhs, expType)
    }

    /**
     * Converts a safe call expression (`receiver?.selector`).
     *
     * The receiver is evaluated once and shared (via `share`) to avoid double evaluation.
     * The result is an [If] that:
     * - Checks `receiver != null`.
     * - If true: converts the selector with the receiver stored as [StmtConversionContext.checkedSafeCallSubject].
     * - If false: returns [NullLit].
     */
    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val selector = safeCallExpression.selector
        val receiver = data.convert(safeCallExpression.receiver)
        val expType = data.embedType(safeCallExpression.resolvedType)
        val checkedSafeCallSubjectType = data.embedType(safeCallExpression.checkedSubjectRef.value.resolvedType)

        return share(receiver) { sharedReceiver ->
            If(
                sharedReceiver.notNullCmp(),
                data.withCheckedSafeCallSubject(sharedReceiver.withType(checkedSafeCallSubjectType)) { convert(selector) },
                NullLit,
                expType
            )
        }
    }

    /**
     * Resolves the checked subject of a safe call (the `it` or implicit `this` inside `?.`).
     * The value was stored in [StmtConversionContext.checkedSafeCallSubject] when entering the
     * safe-call selector; throws if it is absent (indicates a conversion ordering bug).
     */
    override fun visitCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: StmtConversionContext,
    ): ExpEmbedding = data.checkedSafeCallSubject ?: throw SnaktInternalException(
        checkedSafeCallSubject.source,
        "Trying to resolve checked subject $checkedSafeCallSubject which was not captured in StmtConversionContext"
    )

    /**
     * Handles FIR nodes that do not yet have an embedding.
     *
     * Behaviour depends on [UnsupportedFeatureBehaviour]:
     * - [UnsupportedFeatureBehaviour.THROW_EXCEPTION]: throws [SnaktInternalException] (strict mode).
     * - [UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE]: records a minor error and returns
     *   [ErrorExp] so that conversion can continue (lenient mode).
     */
    private fun handleUnimplementedElement(
        source: KtSourceElement?, msg: String, data: StmtConversionContext
    ): ExpEmbedding = when (data.config.behaviour) {
        UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
            throw SnaktInternalException(source, msg)

        UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
            data.errorCollector.addMinorError(msg)
            ErrorExp
        }
    }
}
