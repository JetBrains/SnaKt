/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.CONTEXT_LOCALITY_MISMATCH
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors.LOCALITY_MISMATCH
import org.jetbrains.kotlin.formver.type.plugin.AssignmentTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.CallTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.PropertyTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.QualifiedAccessTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.ReturnTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.ThrowTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.ValueParameterTypeChecker

val AssignmentLocalityChecker = AssignmentTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityJudgment,
    expressionTypeResolver = ExpressionLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH,
)

val CallLocalityChecker = CallTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityJudgment,
    expressionTypeResolver = ExpressionLocalityResolver,
    callParametersTypeResolver = CallParametersLocalityResolver,
    argumentDiagnosticFactory = LOCALITY_MISMATCH,
    contextDiagnosticFactory = CONTEXT_LOCALITY_MISMATCH,
)

val PropertyLocalityChecker = PropertyTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityJudgment,
    expressionTypeResolver = ExpressionLocalityResolver,
    variableTypeResolver = VariableLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)

val QualifiedAccessLocalityChecker = QualifiedAccessTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityJudgment,
    expressionTypeResolver = ExpressionLocalityResolver,
    receiverTypeResolver = ReceiverLocalityResolver,
    variableTypeResolver = VariableLocalityResolver,
    receiverDiagnosticFactory = LOCALITY_MISMATCH,
    contextArgumentDiagnosticFactory = CONTEXT_LOCALITY_MISMATCH,
)

val ReturnLocalityChecker = ReturnTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityJudgment,
    expressionTypeResolver = ExpressionLocalityResolver,
    returnResultTypeResolver = ReturnResultLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)

val ThrowLocalityChecker = ThrowTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityJudgment,
    expressionTypeResolver = ExpressionLocalityResolver,
    throwExceptionTypeResolver = ThrowExceptionLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)

val ValueParameterLocalityChecker = ValueParameterTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityJudgment,
    expressionTypeResolver = ExpressionLocalityResolver,
    parameterDeclaredTypeResolver = VariableLocalityResolver,
    diagnosticFactory = LOCALITY_MISMATCH
)
