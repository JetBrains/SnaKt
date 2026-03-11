/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.formver.BuildConfig
import org.jetbrains.kotlin.formver.common.FormalVerificationPluginNames
import org.jetbrains.kotlin.formver.gradle.model.builder.FormVerModelBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import javax.inject.Inject

/**
 * Gradle [KotlinCompilerPluginSupportPlugin] that wires the FormVer compiler plugin into
 * the Kotlin compilation pipeline.
 *
 * Responsibilities:
 * - Registers the [FormVerExtension] DSL block (`formver { … }`) on the target project.
 * - Registers [FormVerModelBuilder] with the Gradle tooling API so that IDEs can query
 *   the plugin's configuration model.
 * - Translates each property of [FormVerExtension] into a [SubpluginOption] and passes
 *   the full list to the Kotlin compiler via [applyToCompilation].
 * - Reports the compiler plugin artifact coordinates (group, name, version) sourced from
 *   the generated [BuildConfig] so that Gradle can download the plugin from the
 *   configured repositories.
 *
 * The plugin is applicable to every [KotlinCompilation] ([isApplicable] always returns `true`).
 */
class FormVerGradleSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry,
) : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("formver", FormVerExtension::class.java)
        registry.register(FormVerModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val formVerExtension = project.extensions.getByType(FormVerExtension::class.java)

        return project.provider {
            val options = mutableListOf<SubpluginOption>(
                SubpluginOption(FormalVerificationPluginNames.PROJECT_DIR_OPTION_NAME, project.projectDir.absolutePath),
            )

            formVerExtension.myLogLevel?.let {
                options += SubpluginOption(FormalVerificationPluginNames.LOG_LEVEL_OPTION_NAME, it)
            }

            formVerExtension.myErrorStyle?.let {
                options += SubpluginOption(FormalVerificationPluginNames.ERROR_STYLE_NAME, it)
            }

            formVerExtension.myUnsupportedFeatureBehaviour?.let {
                options += SubpluginOption(FormalVerificationPluginNames.UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION_NAME, it)
            }

            formVerExtension.myConversionTargetsSelection?.let {
                options += SubpluginOption(FormalVerificationPluginNames.CONVERSION_TARGETS_SELECTION_OPTION_NAME, it)
            }

            formVerExtension.myVerificationTargetsSelection?.let {
                options += SubpluginOption(FormalVerificationPluginNames.VERIFICATION_TARGETS_SELECTION_OPTION_NAME, it)
            }

            formVerExtension.myDumpViperFiles?.let {
                options += SubpluginOption(FormalVerificationPluginNames.DUMP_VIPER_FILES_OPTION_NAME, it)
            }

            options
        }
    }

    override fun getCompilerPluginId(): String = FormalVerificationPluginNames.PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            BuildConfig.COMPILER_PLUGIN_GROUP,
            BuildConfig.COMPILER_PLUGIN_NAME,
            BuildConfig.COMPILER_PLUGIN_VERSION
        )
}