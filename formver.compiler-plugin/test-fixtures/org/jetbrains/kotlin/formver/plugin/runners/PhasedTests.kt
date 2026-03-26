package org.jetbrains.kotlin.formver.plugin.runners

import org.jetbrains.kotlin.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
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
    val verificationDiagnostics: Map<FirFile, List<KtDiagnostic>>,
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

    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): ViperVerificationArtifact {
        val verificationDiagnostics = linkedMapOf<FirFile, MutableList<KtDiagnostic>>()

        inputArtifact.allFirFiles.values.forEach { firFile ->
            firFile.accept(object : FirDefaultVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    when (element) {
                        is FirSimpleFunction -> visitSimpleFunction(element)
                        else -> element.acceptChildren(this)
                    }

                }

                override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
                    val diagnostics = verifyFunction(simpleFunction, module)
                    if (diagnostics.isNotEmpty()) {
                        verificationDiagnostics.getOrPut(firFile) { mutableListOf() }.addAll(diagnostics)
                    }
                }
            })
        }

        return ViperVerificationArtifact(
            conversionArtifacts = inputArtifact,
            verificationDiagnostics = verificationDiagnostics.mapValues { it.value.toList() }
        )
    }

    @OptIn(TestInfrastructureInternals::class, InternalDiagnosticFactoryMethod::class)
    private fun verifyFunction(
        decl: FirSimpleFunction,
        module: TestModule
    ): List<KtDiagnostic> {
        val program = decl.viperProgram ?: return emptyList()

        val results = mutableListOf<KtDiagnostic>()
        val onFailure = { err: VerifierError ->
            val source = err.position.unwrapOr { decl.source }!!
            val diagnostics = PluginErrors.VIPER_VERIFICATION_ERROR.on(
                source,
                err.msg,
                positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
                languageVersionSettings = module.languageVersionSettings
            )
            val unused = results.add(diagnostics!!)
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

        return results
    }

    override fun shouldTransform(module: TestModule): Boolean = true
}

class ViperResultHandler(testServices: TestServices) :
    AnalysisHandler<ViperVerificationArtifact>(testServices, false, false) {
    override val artifactKind: TestArtifactKind<ViperVerificationArtifact> get() = ViperArtifactKind

    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT)

    override fun processModule(module: TestModule, info: ViperVerificationArtifact) {
        val frontendDiagnostics =
            testServices.firDiagnosticCollectorService.getFrontendDiagnosticsForModule(info.conversionArtifacts)

        for (part in info.conversionArtifacts.partsForDependsOnModules) {
            for (file in part.module.files) {
                val firFile = info.conversionArtifacts.mainFirFiles[file] ?: continue
                val diagnostics = buildList {
                    addAll(frontendDiagnostics[firFile].map { it.diagnostic })
                    addAll(info.verificationDiagnostics[firFile].orEmpty())
                }
                fullDiagnosticsRenderer.storeFullDiagnosticRender(module, diagnostics, file)
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".fir.diag.txt")
    }
}
