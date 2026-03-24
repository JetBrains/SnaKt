package org.jetbrains.kotlin.formver.plugin.runners


import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.formver.plugin.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.formver.plugin.services.PluginAnnotationsProvider
import org.jetbrains.kotlin.formver.plugin.services.StdlibReplacementsProvider
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
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
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.TestPhase


// TODO: Remove the backend part
abstract class AbstractPhasedDiagnosticTest() : AbstractPhasedJvmDiagnosticLightTreeTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.FRONTEND // Is this correct?
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

        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            useHandlers(::NoFirCompilationErrorsHandler, ::TagsGeneratorChecker)
        }

        configureIrHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler,
                ::NoIrCompilationErrorsHandler,
            )
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

private object ViperProgram : FirDeclarationDataKey()

var FirSimpleFunction.viperProgram: Program? by FirDeclarationDataRegistry.data(ViperProgram)
