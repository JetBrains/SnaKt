package org.jetbrains.kotlin.formver.plugin.services

import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoRenderer
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithoutSource
import org.jetbrains.kotlin.formver.plugin.compiler.VerificationErrors
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaInfo
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirMetaInfoUtils
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.*


val TestServices.tagCollector: TagCollector by TestServices.testServiceAccessor()

/**
 * Collects and asserts the tags in the test files.
 */
class TagCollector(
    val testServices: TestServices,
) : TestService {

    private var reportedInfos: MutableMap<TestFile, MutableList<CodeMetaInfo>> = mutableMapOf()
    private lateinit var existingInfos: Map<TestFile, List<CodeMetaInfo>>

    private fun reportMetaInfos(file: TestFile, codeMetaInfos: List<CodeMetaInfo>) {
        val infos = reportedInfos.getOrPut(file) { codeMetaInfos.toMutableList() }
        infos += codeMetaInfos
    }

    fun parseExistingMetadataInfosFromAllSources() {
        existingInfos = buildMap {
            for (file in testServices.moduleStructure.modules.flatMap { it.files }) {
                put(file, CodeMetaInfoParser.getCodeMetaInfoFromText(file.originalContent))
            }
        }
    }

    fun reportDiagnostics(file: TestFile, codeMetaInfos: List<KtDiagnostic>) {
        reportMetaInfos(file, codeMetaInfos.flatMap { it.toMetaInfos() })
    }

    private fun renderText(
        metaInfo: Map<TestFile, List<CodeMetaInfo>>,
        sourceText: (testFile: TestFile) -> String
    ): String {
        val moduleStructure = testServices.moduleStructure
        val builder = StringBuilder()
        // In SplittingModuleTransformerForBoxTests, files may have been assigned to modules in a different order than declared in the original source file.
        // So, to reconstruct the original source file it needs not per-module traversal, but in original file order.
        val filesInOriginalOrder = moduleStructure.modules
            .flatMap { module -> module.files.map { file -> module to file } }
            .sortedBy { (_, file) -> file.startLineNumberInOriginalFile }
        for ((module, file) in filesInOriginalOrder) {
            if (!file.isAdditional) {
                val codeMetaInfos = metaInfo.getValue(file)
                val fileBuilder = StringBuilder()
                val source = sourceText(file)
                CodeMetaInfoRenderer.renderTagsToText(
                    fileBuilder,
                    codeMetaInfos,
                    source
                )
                val reverseTransformers =
                    testServices.sourceFileProvider.preprocessors.filterIsInstance<ReversibleSourceFilePreprocessor>()
                val initialFileContent = fileBuilder.stripAdditionalEmptyLines(file).toString()
                val actualFileContent =
                    reverseTransformers.foldRight(initialFileContent) { transformer, source ->
                        transformer.revert(
                            file,
                            source
                        )
                    }
                builder.append(actualFileContent)
            }
        }
        val actualText = builder.toString()
        return actualText
    }

    fun sourceFileWithoutTags(file: TestFile): String {
        val content = testServices.sourceFileProvider.getContentOfSourceFile(file)
        return content.replace(
            Regex("\"<![A-Z_]*!>(.*?)<!>\"")
        ) { it.groupValues[1] }
    }

    /**
     * Asserts equality of the tags and the expected output.
     */
    fun assertAll() {
        testServices.assertions.assertEqualsToFile(
            testServices.moduleStructure.originalTestDataFiles.single(),
            renderText(reportedInfos, testServices.sourceFileProvider::getContentOfSourceFile)
        )
    }

    /**
     * Removes the verification error marker from the expected output.
     */
    private fun expectedFileWithoutVerificationError(): String {
        parseExistingMetadataInfosFromAllSources()
        val existingInfosWithoutVerification = existingInfos.mapValues { it ->
            it.value.filter { it.tag !in VerificationErrors.tags() }
        }

        return renderText(existingInfosWithoutVerification, this::sourceFileWithoutTags)
    }

    /**
     * Asserts equality of the tags (without verification error tags)
     */
    fun assertConversion() {
        val expectedOutput = expectedFileWithoutVerificationError()
        val actualOutput = renderText(reportedInfos, testServices.sourceFileProvider::getContentOfSourceFile)
        testServices.assertions.assertEquals(expectedOutput, actualOutput) {
            "Actual tags differ from golden file"
        }
    }

    private fun StringBuilder.stripAdditionalEmptyLines(file: TestFile): CharSequence {
        return if (file.startLineNumberInOriginalFile != 0) {
            this.removePrefix((1..file.startLineNumberInOriginalFile).joinToString(separator = "") { "\n" })
        } else {
            this.toString()
        }
    }
}


private fun KtDiagnostic.toMetaInfos(): List<FirDiagnosticCodeMetaInfo> {
    val ranges = when (this) {
        is KtDiagnosticWithSource -> textRanges
        is KtDiagnosticWithoutSource -> listOf(firstRange)
    }
    return ranges.map { range ->
        val metaInfo = FirDiagnosticCodeMetaInfo(this, FirMetaInfoUtils.renderDiagnosticNoArgs, range)
        metaInfo
    }
}