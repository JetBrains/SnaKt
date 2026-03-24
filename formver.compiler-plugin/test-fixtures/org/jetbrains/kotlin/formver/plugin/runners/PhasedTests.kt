package org.jetbrains.kotlin.formver.plugin.runners

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.formver.core.shouldVerify
import org.jetbrains.kotlin.formver.core.viperProgram
import org.jetbrains.kotlin.formver.plugin.compiler.PluginErrors
import org.jetbrains.kotlin.formver.plugin.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.formver.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.formver.plugin.services.StdlibReplacementsProvider
import org.jetbrains.kotlin.formver.viper.Verifier
import org.jetbrains.kotlin.formver.viper.ast.unwrapOr
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.configuration.configureCommonDiagnosticTestPaths
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LATEST_LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly


abstract class AbstractPhasedDiagnosticTest() : AbstractPhasedJvmDiagnosticLightTreeTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.FRONTEND
            +ENABLE_PLUGIN_PHASES
            +RENDER_DIAGNOSTICS_FULL_TEXT
            LANGUAGE with "+PropertyParamAnnotationDefaultTargetMode"
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
        }
        commonServicesConfigurationForCodegenAndDebugTest(FrontendKinds.FIR)

        facadeStep(::FirCliJvmFacade)
        firHandlersStep {
            useHandlers(::AfterConversionHandler)
        }

        facadeStep(::ViperProgramVerificationFacade)
        handlersStep(
            ViperArtifactKind,
            compilationStage = CompilationStage.FIRST
        ) {
            useHandlers(::ViperResultHandler)
        }

        configureFirParser(parser)
        configureCommonDiagnosticTestPaths()

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

/**
 * This renderer compares the diagnostics to the golden file ignoring verification diagnostics
 * Most of the code is copied form [FullDiagnosticsRenderer]
 */
class WithoutVerificationDiagnosticsRenderer(private val directive: SimpleDirective) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

    fun assertCollectedDiagnostics(testServices: TestServices, expectedExtension: String) {
        val directives = testServices.moduleStructure.allDirectives
        if (USE_LATEST_LANGUAGE_VERSION in directives) return

        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile =
            testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".fir")}$expectedExtension")
        val expectedOutput = mutableListOf<String>()
        var lastWasViper = false
        for (line in expectedFile.readLines()) {
            if (lastWasViper) {
                lastWasViper = false
                continue
            }
            if (line.contains("Viper verification error:")) {
                lastWasViper = true
                continue
            }
            expectedOutput.add(line)
        }

        if (directive !in directives) {
            if (DiagnosticsDirectives.RENDER_ALL_DIAGNOSTICS_FULL_TEXT !in directives) {
                testServices.assertions.assertFileDoesntExist(expectedFile, directive)
            }
            return
        }
        if (dumper.isEmpty() && !expectedFile.exists()) {
            return
        }
        val resultDump = dumper.generateResultingDump()

        testServices.assertions.assertEquals(expectedOutput.joinToString("\n"), resultDump, {
            "Actual data differs from file content"
        })
    }

    fun storeFullDiagnosticRender(module: TestModule, diagnostics: List<KtDiagnostic>, file: TestFile) {
        if (directive !in module.directives) return
        if (diagnostics.isEmpty()) return

        class DiagnosticData(val textRanges: List<TextRange>, val severity: String, val message: String)

        val reportedDiagnostics = diagnostics
            .map {
                DiagnosticData(
                    textRanges = when (it) {
                        is KtDiagnosticWithSource -> it.textRanges
                        is KtDiagnosticWithoutSource -> listOf(it.firstRange)
                    },
                    severity = AnalyzerWithCompilerReport.convertSeverity(it.severity).toString()
                        .toLowerCaseAsciiOnly(),
                    message = it.renderMessage()
                )
            }
            .sortedWith(compareBy<DiagnosticData> { it.textRanges.first().startOffset }.thenBy { it.message })

        dumper.builderForModule(module).appendLine(reportedDiagnostics.joinToString(separator = "\n\n") {
            "/${file.name}:${it.textRanges.first()}: ${it.severity}: ${it.message}"
        })
    }
}

/**
 * This handler runs after the conversion phase and checks the diagnostics, ignoring verification diagnostics.
 * Most of the code is copied form [FirDiagnosticsHandler]
 */
class AfterConversionHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {

    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    override val directiveContainers: List<DirectivesContainer> =
        listOf(DiagnosticsDirectives)

    override val additionalServices: List<ServiceRegistrationData> =
        listOf(service(::DiagnosticsService), service(::FirDiagnosticCollectorService))

    private val renderer = WithoutVerificationDiagnosticsRenderer(RENDER_DIAGNOSTICS_FULL_TEXT)

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        renderer.assertCollectedDiagnostics(testServices, ".fir.diag.txt")
    }

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val frontendDiagnosticsPerFile =
            testServices.firDiagnosticCollectorService.getFrontendDiagnosticsForModule(info)

        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            val lightTreeComparingModeEnabled =
                FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in currentModule.directives
            val lightTreeEnabled =
                currentModule.directives.singleValue(FirDiagnosticsDirectives.FIR_PARSER) == FirParser.LightTree
            val forceRenderArguments = FirDiagnosticsDirectives.RENDER_DIAGNOSTICS_MESSAGES in currentModule.directives

            for (file in currentModule.files) {
                val firFile = info.mainFirFiles[file] ?: continue
                val diagnostics = frontendDiagnosticsPerFile[firFile]
                val diagnosticsMetadataInfos = diagnostics
                    .groupBy({ it.kmpCompilationMode }, { it.diagnostic })
                    .flatMap { (kmpCompilation, diagnostics) ->
                        diagnostics.diagnosticCodeMetaInfos(
                            currentModule, file,
                            diagnosticsService, globalMetadataInfoHandler,
                            lightTreeEnabled, lightTreeComparingModeEnabled,
                            forceRenderArguments,
                            kmpCompilation
                        )
                    }
                globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetadataInfos)
                renderer.storeFullDiagnosticRender(module, diagnostics.map { it.diagnostic }, file)
            }
        }
    }
}

// VERIFICATION STEP

object ViperArtifactKind : TestArtifactKind<ViperVerificationArtifact>("ViperVerification")

class ViperVerificationArtifact(
    val conversionArtifacts: FirOutputArtifact,
    val verificationDiagnostics: Map<FirFile, List<KtDiagnostic>>,
    val verificationSkippedForFiles: Set<FirFile>
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

    @OptIn(TestInfrastructureInternals::class)
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): ViperVerificationArtifact {
        val verificationDiagnostics = linkedMapOf<FirFile, MutableList<KtDiagnostic>>()
        val verificationSkippedForFiles = mutableSetOf<FirFile>()
        inputArtifact.allFirFiles.values.forEach { firFile ->
            firFile.accept(object : FirDefaultVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    when (element) {
                        is FirSimpleFunction -> visitSimpleFunction(element)
                        else -> element.acceptChildren(this)
                    }

                }

                override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
                    if (simpleFunction.shouldVerify == true) {
                        val diagnostics = verifyFunction(simpleFunction, module)
                        if (diagnostics.isNotEmpty()) {
                            verificationDiagnostics.getOrPut(firFile) { mutableListOf() }.addAll(diagnostics)
                        }
                    }
                }
            })
        }

        return ViperVerificationArtifact(
            conversionArtifacts = inputArtifact,
            verificationDiagnostics = verificationDiagnostics.mapValues { it.value.toList() },
            verificationSkippedForFiles
        )
    }

    @OptIn(InternalDiagnosticFactoryMethod::class)
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

        val verifier = Verifier()
        try {
            verifier.verify(program, onFailure)
        } finally {
            verifier.stop()
        }

        return results
    }

    override fun shouldTransform(module: TestModule): Boolean = true

    fun hasErrorInConversion(module: TestModule, firFile: FirFile, inputArtifact: FirOutputArtifact): Boolean {
        return testServices.firDiagnosticCollectorService.getFrontendDiagnosticsForModule(inputArtifact)[firFile].isNotEmpty()
    }
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
