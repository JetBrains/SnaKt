package org.jetbrains.kotlin.formver.plugin.runners


import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.formver.core.viperProgram
import org.jetbrains.kotlin.formver.plugin.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.formver.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.formver.plugin.services.StdlibReplacementsProvider
import org.jetbrains.kotlin.formver.viper.Verifier
import org.jetbrains.kotlin.formver.viper.ast.unwrapOr
import org.jetbrains.kotlin.formver.viper.errors.GenericConsistencyError
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.configuration.configureCommonDiagnosticTestPaths
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker
import org.jetbrains.kotlin.test.frontend.fir.handlers.FullDiagnosticsRenderer
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.services.*
import kotlin.collections.map

// TODO: Remove the backend part
abstract class AbstractPhasedDiagnosticTest() : AbstractPhasedJvmDiagnosticLightTreeTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.FRONTEND// Is this correct?
            +ENABLE_PLUGIN_PHASES
            +RENDER_DIAGNOSTICS_FULL_TEXT
            LANGUAGE with "+PropertyParamAnnotationDefaultTargetMode"
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
        }
        commonServicesConfigurationForCodegenAndDebugTest(FrontendKinds.FIR)
        facadeStep(::FirCliJvmFacade)
        classicFrontendHandlersStep()
        firHandlersStep()
        facadeStep(::ViperProgramVerificationFacade)
        handlersStep(
            ViperArtifactKind,
            compilationStage = CompilationStage.FIRST
        ) {
            useHandlers(::ViperResultHandler)
        }

        configureFirParser(parser)
        configureCommonDiagnosticTestPaths()


        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            // Standard checkers logic
            useHandlers(::NoFirCompilationErrorsHandler, ::TagsGeneratorChecker)

        }


        useAdditionalSourceProviders(::StdlibReplacementsProvider)

        useConfigurators(
            ::PluginAnnotationsProvider,
            ::ExtensionRegistrarConfigurator
        )

    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}

object ViperArtifactKind : TestArtifactKind<ViperVerificationArtifact>("ViperVerification")

class ViperVerificationArtifact(
    val results: Map<TestFile, Map<KtSourceElement, MutableList<String>>>
) : ResultingArtifact<ViperVerificationArtifact>() {
    override val kind: TestArtifactKind<ViperVerificationArtifact>
        get() = ViperArtifactKind
}


class ViperProgramVerificationFacade(val testServices: TestServices) :
    AbstractTestFacade<FirOutputArtifact, ViperVerificationArtifact>() {
    override val inputKind: TestArtifactKind<FirOutputArtifact>
        get() = FrontendKinds.FIR
    override val outputKind: ViperArtifactKind
        get() = ViperArtifactKind


    @OptIn(DirectDeclarationsAccess::class)
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): ViperVerificationArtifact {
        val frontendDiagnosticsPerFile =
            testServices.firDiagnosticCollectorService.getFrontendDiagnosticsForModule(inputArtifact)
        val result: Map<TestFile, Map<KtSourceElement, MutableList<String>>> =
            inputArtifact.allFirFiles.map { (file, firFile) ->
                val res = firFile.declarations
                    .map { decl ->
                        if (decl is FirSimpleFunction) verifyFunction(decl)
                        else emptyMap()
                    }
                    // This flattens the List<Map<K, V>> into a single Map<K, V>
                    .fold(mutableMapOf<KtSourceElement, MutableList<String>>()) { acc, map ->
                        acc.apply { putAll(map) }
                    }
                file to res
            }.toMap()
        return ViperVerificationArtifact(result)
    }

    @OptIn(TestInfrastructureInternals::class)
    private fun verifyFunction(decl: FirSimpleFunction): Map<KtSourceElement, MutableList<String>> {
        val program = decl.viperProgram!!
        val result = mutableMapOf<KtSourceElement, MutableList<String>>()

        val onFailure = { err: VerifierError ->
            val source = err.position.unwrapOr { decl.source }!!
            val ins = result.getOrElse(source) { mutableListOf() }.add(err.msg)
        }
        val consistencyErrors = program.checkTransitively()
        for (error in consistencyErrors) {
            onFailure(GenericConsistencyError(error))
        }
        val verifier = Verifier()
        try {
            verifier.verify(program, onFailure)
            result.getOrElse(decl.source!!) { mutableListOf() }.add("Verification completed successfully")
        } finally {
            verifier.stop()
        }
        return result
    }

    override fun shouldTransform(module: TestModule): Boolean = true
}

class ViperResultHandler(testServices: TestServices) :
    AnalysisHandler<ViperVerificationArtifact>(testServices, false, false) {
    override val artifactKind: TestArtifactKind<ViperVerificationArtifact> get() = ViperArtifactKind

    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT)

    override fun processModule(module: TestModule, info: ViperVerificationArtifact) {
        module.files.map { file ->
            val verificationResult = info.results[file]
            val diagnostics = verificationResult?.values?.flatten() ?: emptyList()
        }
        fullDiagnosticsRenderer.storeFullDiagnosticRender(module, diagnostics.map { it.diagnostic }, file)
    }
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".verify.diag.txt")
    }
}