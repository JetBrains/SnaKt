/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.locality.contract.plugin

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.formver.type.plugin.CallTypeChecker
import org.jetbrains.kotlin.formver.locality.contract.plugin.LocalityContractErrors.CONTEXT_LOCALITY_CONTRACT_MISMATCH
import org.jetbrains.kotlin.formver.locality.contract.plugin.LocalityContractErrors.LOCALITY_CONTRACT_MISMATCH
import org.jetbrains.kotlin.formver.type.plugin.AssignmentTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.PropertyTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.QualifiedAccessTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.ReturnTypeChecker
import org.jetbrains.kotlin.formver.type.plugin.ValueParameterTypeChecker

val AssignmentLocalityContractChecker = AssignmentTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityContractJudgment,
    expressionTypeResolver = ExpressionLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)

val CallLocalityContractChecker = CallTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityContractJudgment,
    expressionTypeResolver = ExpressionLocalityContractResolver,
    callParametersTypeResolver = CallParametersLocalityContractResolver,
    argumentDiagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
    contextDiagnosticFactory = CONTEXT_LOCALITY_CONTRACT_MISMATCH
)

val PropertyLocalityContractChecker = PropertyTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityContractJudgment,
    expressionTypeResolver = ExpressionLocalityContractResolver,
    variableTypeResolver = VariableLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)

val QualifiedAccessLocalityContractChecker = QualifiedAccessTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityContractJudgment,
    expressionTypeResolver = ExpressionLocalityContractResolver,
    receiverTypeResolver = ReceiverLocalityContractResolver,
    variableTypeResolver = VariableLocalityContractResolver,
    receiverDiagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
    contextArgumentDiagnosticFactory = CONTEXT_LOCALITY_CONTRACT_MISMATCH,
)

val ReturnLocalityContractChecker = ReturnTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityContractJudgment,
    expressionTypeResolver = ExpressionLocalityContractResolver,
    returnResultTypeResolver = ReturnResultLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)

val ValueParameterLocalityContractChecker = ValueParameterTypeChecker(
    kind = MppCheckerKind.Common,
    typeJudgment = LocalityContractJudgment,
    expressionTypeResolver = ExpressionLocalityContractResolver,
    parameterDeclaredTypeResolver = VariableLocalityContractResolver,
    diagnosticFactory = LOCALITY_CONTRACT_MISMATCH,
)
