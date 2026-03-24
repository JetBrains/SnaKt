package org.jetbrains.kotlin.formver.plugin.runners


import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.formver.core.viperProgram
import org.jetbrains.kotlin.formver.plugin.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.formver.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.formver.plugin.services.StdlibReplacementsProvider
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.configureCommonDiagnosticTestPaths
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.services.*

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
        commonConfigurationForJvmTest(
            targetFrontend = FrontendKinds.FIR,
            frontendFacade = ::FirCliJvmFacade,
            frontendToBackendConverter = ::Fir2IrCliJvmFacade, // How to remove this? Or is it enough to have TestPhase.FRONTEND?
            backendFacade = ::BackendCliJvmFacade
        )
        configureFirParser(parser)
        configureCommonDiagnosticTestPaths()


        /*        configureIrHandlersStep {
                    useHandlers(
                        ::IrDiagnosticsHandler,
                        ::NoIrCompilationErrorsHandler,
                    )
                }*/

        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            // Standard checkers logic
            useHandlers(::NoFirCompilationErrorsHandler, ::TagsGeneratorChecker)

        }

        facadeStep(::ViperProgramVerificationFacade)
        handlersStep(ViperArtifactKind, CompilationStage.FIRST) {
            useHandlers(::ViperResultHandler)
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
    val results: Map<FirSimpleFunction, Boolean>
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
        inputArtifact.allFirFiles.map { (file, firFile) ->
            firFile.declarations.forEach { decl ->
                when (decl) {
                    is FirSimpleFunction -> verifyFunction(decl.viperProgram!!)
                    else -> {}
                }
            }
        }
        return ViperVerificationArtifact(mapOf())
    }

    @OptIn(TestInfrastructureInternals::class)
    private fun verifyFunction(program: viper.silver.ast.Program) {
        val reporter = testServices.firDiagnosticCollectorService
        /*val collector = testServices.testConfiguration.testServices.diagnosticsService*/
        // Here we might want to add diagnostics that some assertion does not hold (output of the viper verification)
    }

    override fun shouldTransform(module: TestModule): Boolean = true
}

class ViperResultHandler(testServices: TestServices) :
    AnalysisHandler<ViperVerificationArtifact>(testServices, false, false) {
    override val artifactKind: TestArtifactKind<ViperVerificationArtifact> get() = ViperArtifactKind

    override fun processModule(module: TestModule, info: ViperVerificationArtifact) {
        println("handler")
        info.results.forEach { (function, status) ->
            if (status) {
                println("Function ${function.name} verified successfully!")
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}