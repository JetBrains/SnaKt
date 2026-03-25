package org.jetbrains.kotlin.formver.plugin.runners


import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.pipeline.collectLostDiagnosticsOnFile
import org.jetbrains.kotlin.formver.core.viperProgram
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors
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
import org.jetbrains.kotlin.test.frontend.fir.FirCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker
import org.jetbrains.kotlin.test.frontend.fir.handlers.FullDiagnosticsRenderer
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
    val conversionArtifacts: FirOutputArtifact,
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
        val scopeSession = inputArtifact.partsForDependsOnModules[0].scopeSession
        val globalReporter = testServices.firDiagnosticCollectorService.reporterForLTSyntaxErrors
        var outputArtifacts = inputArtifact as FirCliBasedOutputArtifact<*>
        val reporter = DiagnosticReporterFactory.createReporter(BaseDiagnosticsCollector.RawReporter.DO_NOTHING)
        inputArtifact.allFirFiles.mapValues { it ->
            val firFile = it.value
            val fileMessages = mutableMapOf<KtSourceElement, MutableList<String>>()

            val session = firFile.moduleData.session
            firFile.declarations.forEach { declaration ->
                if (declaration is FirSimpleFunction) {
                    val functionResults = verifyFunction(declaration, module)
                    functionResults.forEach {

                        val byFilePath = session.collectLostDiagnosticsOnFile(
                            scopeSession,
                            firFile,
                            globalReporter
                        )
                        outputArtifacts.cliArtifact.diagnosticCollector.diagnosticsByFilePath

                    }
                }
            }
            fileMessages
        }
        return ViperVerificationArtifact(inputArtifact)
    }

    @OptIn(TestInfrastructureInternals::class, InternalDiagnosticFactoryMethod::class)
    private fun verifyFunction(
        decl: FirSimpleFunction,
        module: TestModule
    ): List<KtDiagnosticWithParameters1<String>> {
        val program = decl.viperProgram!!

        val results = mutableListOf<KtDiagnosticWithParameters1<String>>()
        val onFailure = { err: VerifierError ->
            val source = err.position.unwrapOr { decl.source }!!
//            val unsued = result.getOrPut(source) { mutableListOf() }.add(err.msg)
            val diagnostics = PluginErrors.VIPER_VERIFICATION_ERROR.on(
                source,
                err.msg,
                positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                languageVersionSettings = module.languageVersionSettings
            )
            val unsued = results.add(diagnostics!!)
        }
        val consistencyErrors = program.checkTransitively()
        for (error in consistencyErrors) {
            onFailure(GenericConsistencyError(error))
        }
        val verifier = Verifier()
        try {
            verifier.verify(program, onFailure)
        } finally {
            verifier.stop()
        }

        val diagnostics = PluginErrors.VIPER_VERIFICATION_ERROR.on(
            decl.source!!,
            "hello worlds",
            positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
            languageVersionSettings = module.languageVersionSettings
        )
        val unsued = results.add(diagnostics!!)


        return results
    }

    override fun shouldTransform(module: TestModule): Boolean = true
}

class ViperResultHandler(testServices: TestServices) :
    AnalysisHandler<ViperVerificationArtifact>(testServices, false, false) {
    override val artifactKind: TestArtifactKind<ViperVerificationArtifact> get() = ViperArtifactKind

    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT)

    override fun processModule(module: TestModule, info: ViperVerificationArtifact) {
        println(info)
    }
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".verify.diag.txt")
    }
}