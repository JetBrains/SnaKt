/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.formver.type.plugin.AssignmentTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.CallTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.PropertyTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.ReturnTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.ThrowTypeChecker

val AssignmentUniquenessChecker = AssignmentTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = UniquenessJudgment,
    expressionTypeResolver = ExpressionUniquenessResolver,
    leftTypeResolver = ExpressionDefaultUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)

val CallUniquenessChecker = CallTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = UniquenessJudgment,
    expressionTypeResolver = ExpressionUniquenessResolver,
    callParametersTypeResolver = CallParametersUniquenessResolver,
    argumentDiagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
    contextDiagnosticFactory = UniquenessErrors.CONTEXT_UNIQUENESS_MISMATCH,
)

val PropertyUniquenessChecker = PropertyTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = UniquenessJudgment,
    expressionTypeResolver = ExpressionUniquenessResolver,
    variableTypeResolver = VariableUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)

val ReturnUniquenessChecker = ReturnTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = UniquenessJudgment,
    expressionTypeResolver = ExpressionUniquenessResolver,
    returnResultTypeResolver = ReturnResultUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)

val ThrowUniquenessChecker = ThrowTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = UniquenessJudgment,
    expressionTypeResolver = ExpressionUniquenessResolver,
    throwExceptionTypeResolver = ThrowExceptionUniquenessResolver,
    diagnosticFactory = UniquenessErrors.UNIQUENESS_MISMATCH,
)
