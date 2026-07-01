/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.formver.type.plugin.AssignmentTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.CallTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.PropertyTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.ReturnTypeFactChecker
import org.jetbrains.kotlin.formver.type.plugin.ThrowTypeFactChecker

val AssignmentUniquenessChecker = AssignmentTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = UniquenessJudgment,
    expressionTypeFactResolver = ExpressionUniquenessResolver,
    leftTypeFactResolver = ExpressionDefaultUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)

val CallUniquenessChecker = CallTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = UniquenessJudgment,
    expressionTypeFactResolver = ExpressionUniquenessResolver,
    callArgumentTypeFactsMapper = CallParametersUniquenessResolver,
    argumentDiagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
    contextDiagnosticFactory = UniquenessErrors.CONTEXT_UNIQUENESS_MISMATCH,
)

val PropertyUniquenessChecker = PropertyTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = UniquenessJudgment,
    expressionTypeFactResolver = ExpressionUniquenessResolver,
    variableTypeFactResolver = VariableUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)

val ReturnUniquenessChecker = ReturnTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = UniquenessJudgment,
    expressionTypeFactResolver = ExpressionUniquenessResolver,
    returnResultTypeFactResolver = ReturnResultUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)

val ThrowUniquenessChecker = ThrowTypeFactChecker(
    kind = MppCheckerKind.Common,
    typeFactJudgment = UniquenessJudgment,
    expressionTypeFactResolver = ExpressionUniquenessResolver,
    throwExceptionTypeFactResolver = ThrowExceptionUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)
