/*
 * Copyright 2025 gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.openhealth.plugin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.File
import javax.inject.Inject

/**
 * Represents the style of license headers, including the start,
 * middle, and end markers, and the file extensions it applies to.
 */
class LicenseHeaderStyleExtension(
    val start: Property<String>,
    val middle: Property<String>,
    val end: Property<String>,
    val extensions: SetProperty<String>,
)

/**
 * The `LicenseHeaderExtension` class provides configuration options for managing license headers in project files.
 */
abstract class LicenseHeaderExtension
    @Inject
    constructor(
        private val objectFactory: ObjectFactory,
        root: FileTree,
    ) {
        val commentStyles: ListProperty<LicenseHeaderStyleExtension> =
            objectFactory
                .listProperty(LicenseHeaderStyleExtension::class.java)
                .convention(
                    defaultStyles.map { (extensions, style) ->
                        LicenseHeaderStyleExtension(
                            start = objectFactory.property(String::class.java).convention(style.start),
                            middle = objectFactory.property(String::class.java).convention(style.middle),
                            end = objectFactory.property(String::class.java).convention(style.end),
                            extensions = objectFactory.setProperty(String::class.java).convention(extensions),
                        )
                    },
                )

        /**
         * Adds a custom comment style for license headers.
         *
         * @param action The action to configure the comment style.
         */
        fun commentStyle(action: Action<in LicenseHeaderStyleExtension>) {
            val style =
                LicenseHeaderStyleExtension(
                    start = objectFactory.property(String::class.java),
                    middle = objectFactory.property(String::class.java).convention(""),
                    end = objectFactory.property(String::class.java).convention(""),
                    extensions = objectFactory.setProperty(String::class.java),
                )
            action.execute(style)
            require(style.start.isPresent) { "Start must be provided" }
            require(style.extensions.isPresent) { "Extensions must be provided" }
            commentStyles.add(style)
        }

        /**
         * Adds a custom comment style for license headers.
         *
         * @param start The start marker of the comment style.
         * @param middle The middle marker of the comment style.
         * @param end The end marker of the comment style.
         * @param extensions The file extensions the comment style applies to.
         */
        fun commentStyle(
            start: String,
            middle: String? = null,
            end: String? = null,
            extensions: Set<String>,
        ) {
            require(extensions.isNotEmpty()) { "Extensions must be provided" }
            commentStyle { style ->
                style.start.set(start)
                middle?.also { style.middle.set(it) }
                end?.also { style.end.set(it) }
                style.extensions.set(extensions)
            }
        }

        val filesToScan: ConfigurableFileCollection = objectFactory.fileCollection().from(root)

        internal val header: Property<String> = objectFactory.property(String::class.java).convention("")

        /**
         * Sets the license header text.
         *
         * @param value The license header text.
         */
        fun header(value: String) {
            header.set(value)
            header.finalizeValue()
        }

        internal val dryRun: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

        /**
         * Enables or disables dry run mode.
         *
         * @param value `true` to enable dry run mode, `false` to disable.
         */
        fun dryRun(value: Boolean) {
            dryRun.set(value)
            dryRun.finalizeValue()
        }

        internal val failOnMissing: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

        /**
         * Sets whether to fail on missing or invalid headers.
         *
         * @param value `true` to fail on missing or invalid headers, `false` otherwise.
         */
        fun failOnMissing(value: Boolean) {
            failOnMissing.set(value)
            failOnMissing.finalizeValue()
        }

        internal abstract val variables: MapProperty<String, String>

        fun variables(value: Map<String, String>) {
            variables.putAll(value)
            variables.finalizeValue()
        }

        /**
         * Sets the template variables for the license header.
         *
         * @param value The template variables as pairs.
         */
        fun variables(vararg value: Pair<String, String>) {
            variables(value.toMap())
        }

        /**
         * The `TaskScope` class provides methods to apply, update, remove, and validate license headers in files.
         *
         * @param task The Gradle task.
         */
        @Suppress("TooManyFunctions")
        internal inner class TaskScope(
            task: Task,
        ) {
            private val logger = task.logger

            /**
             * Applies license headers to files.
             */
            fun applyHeaders() {
                processFiles(::handleApply) { file, content ->
                    if (!hasLicenseHeader(content, file)) {
                        val header = formatHeaderForFile(file, processTemplate())
                        writeToFile(file, "$header\n\n$content")
                        logger.info("Added license header to ${file.name}")
                    }
                }
            }

            /**
             * Updates license headers in files.
             */
            fun updateHeaders() {
                processFiles(::handleUpdate) { file, content ->
                    val structure = parseFileContent(file, content)

                    logger.info("Processing file: ${file.name}")
                    logger.info("Extracted header: \n${structure.header ?: "No header found"}")
                    logger.info("Extracted content: \n${structure.content}")

                    if (structure.header != null) {
                        val newHeader = formatHeaderForFile(file, processTemplate())

                        if (structure.header.trim() != newHeader.trim()) {
                            val newContent =
                                buildString {
                                    append(newHeader)
                                    append("\n\n")
                                    append(structure.content)
                                }

                            logger.info("New content to write: \n$newContent")

                            writeToFile(file, newContent)
                            logger.info("Updated license header in ${file.name}")
                        }
                    }
                }
            }

            /**
             * Removes license headers from files.
             */
            fun removeHeaders() {
                processFiles(::handleRemove) { file, content ->
                    val structure = parseFileContent(file, content)
                    if (structure.header != null) {
                        val newContent = structure.content.trimStart()
                        writeToFile(file, newContent)
                        logger.info("Removed license header from ${file.name}")
                    }
                }
            }

            /**
             * Validates license headers in files.
             */
            fun validateHeaders() {
                var hasErrors = false
                processFiles(::handleValidate) { file, content ->
                    val header = extractExistingHeader(file, content)
                    val expectedHeader = formatHeaderForFile(file, processTemplate())

                    when {
                        header == null -> {
                            hasErrors = true
                        }

                        header.trim() != expectedHeader.trim() -> {
                            hasErrors = true
                        }
                    }
                }
                if (hasErrors && failOnMissing.get()) {
                    throw GradleException("License header validation failed")
                }
            }

            private fun parseFileContent(
                file: File,
                content: String,
            ): ContentStructure {
                val lines = content.lines()
                val headerRange = findHeaderRange(file, lines)

                return when {
                    headerRange != null -> {
                        val headerLines = lines.subList(headerRange.first, headerRange.last + 1)
                        ContentStructure(
                            header = headerLines.joinToString("\n"),
                            content = lines.drop(headerRange.last + 1).joinToString("\n").trimStart(),
                        )
                    }
                    else ->
                        ContentStructure(
                            header = null,
                            content = lines.joinToString("\n").trimStart(),
                        )
                }
            }

            @Suppress("ReturnCount")
            private fun findHeaderRange(
                file: File,
                lines: List<String>,
            ): IntRange? {
                val firstNonEmptyIndex =
                    lines
                        .indexOfFirst { it.trim().isNotEmpty() }
                        .takeUnless { it == -1 } ?: return null

                val style = getCommentStyleForFile(file)
                val firstNonEmptyLine = lines[firstNonEmptyIndex].trim()

                if (!firstNonEmptyLine.startsWith(style.start.trim())) {
                    return null
                }

                val lastHeaderLine = findLastHeaderLine(lines, firstNonEmptyIndex, style)
                return IntRange(firstNonEmptyIndex, lastHeaderLine)
            }

            @Suppress("ReturnCount")
            private fun findLastHeaderLine(
                lines: List<String>,
                startIndex: Int,
                style: CommentStyle,
            ): Int {
                var lastHeaderLine = startIndex

                for (i in (startIndex + 1) until lines.size) {
                    val line = lines[i].trim()

                    when {
                        line.isEmpty() -> return i - 1
                        style.end.isNotEmpty() && line.endsWith(style.end) -> return i
                        style.end.isEmpty() && !isValidSingleLineComment(line, style) -> return i - 1
                        else -> lastHeaderLine = i
                    }
                }

                return lastHeaderLine
            }

            private fun isValidSingleLineComment(
                line: String,
                style: CommentStyle,
            ): Boolean = line.startsWith(style.middle.trim()) && line.endsWith(style.start.trim())

            private fun processFiles(
                processFile: (
                    file: File,
                    content: String,
                    structure: ContentStructure,
                    isDryRun: Boolean,
                    action: (File, String) -> Unit,
                ) -> Unit,
                action: (File, String) -> Unit,
            ) {
                val headerText = header.get()
                val isDryRun = dryRun.get()
                val shouldFailOnMissing = failOnMissing.get()

                if (headerText.isBlank()) {
                    logger.warn("No license header provided. Skipping task.")
                    return
                }

                val allExtensions = commentStyles.get().flatMap { it.extensions.get() }

                @Suppress("TooGenericExceptionCaught")
                filesToScan.forEach { file ->
                    if (file.extension in allExtensions) {
                        try {
                            val content = file.readText(Charsets.UTF_8)
                            val structure = parseFileContent(file, content)
                            processFile(file, content, structure, isDryRun, action)
                        } catch (e: Exception) {
                            handleError(file, e, shouldFailOnMissing)
                        }
                    }
                }
            }

            private fun handleApply(
                file: File,
                content: String,
                structure: ContentStructure,
                isDryRun: Boolean,
                action: (File, String) -> Unit,
            ) {
                if (structure.header == null) {
                    logAction(isDryRun, "add", file.name)
                    executeAction(isDryRun, file, content, action)
                } else {
                    logger.info("Skipping ${file.name} - header already exists")
                }
            }

            private fun handleUpdate(
                file: File,
                content: String,
                structure: ContentStructure,
                isDryRun: Boolean,
                action: (File, String) -> Unit,
            ) {
                if (structure.header != null) {
                    val newHeader = formatHeaderForFile(file, processTemplate())
                    if (structure.header.trim() != newHeader.trim()) {
                        logAction(isDryRun, "update", file.name)
                        executeAction(isDryRun, file, content, action)
                    } else {
                        logger.info("Skipping ${file.name} - header is up to date")
                    }
                } else {
                    logger.info("Skipping ${file.name} - no header found")
                }
            }

            private fun handleRemove(
                file: File,
                content: String,
                structure: ContentStructure,
                isDryRun: Boolean,
                action: (File, String) -> Unit,
            ) {
                if (structure.header != null) {
                    logAction(isDryRun, "remove", file.name)
                    executeAction(isDryRun, file, content, action)
                }
            }

            private fun handleValidate(
                file: File,
                content: String,
                structure: ContentStructure,
                isDryRun: Boolean,
                action: (File, String) -> Unit,
            ) {
                val expectedHeader = formatHeaderForFile(file, processTemplate())
                val prefix = if (isDryRun) "[DRY RUN] Would report: " else ""

                val message = validateHeader(file.name, structure.header, expectedHeader, prefix)
                logger.run {
                    if (message.contains("Missing") || message.contains("Invalid")) {
                        error(message)
                    } else {
                        lifecycle(message)
                    }
                }

                executeAction(isDryRun, file, content, action)
            }

            private fun validateHeader(
                fileName: String,
                header: String?,
                expectedHeader: String,
                prefix: String,
            ): String =
                when {
                    header == null -> "${prefix}Missing license header in $fileName"
                    header.trim() != expectedHeader.trim() -> "${prefix}Invalid license header in $fileName"
                    else -> "$fileName - header is valid"
                }

            private fun logAction(
                isDryRun: Boolean,
                action: String,
                fileName: String,
            ) {
                if (isDryRun) {
                    logger.lifecycle("[DRY RUN] $action license header in $fileName")
                } else {
                    logger.info("Going to $action license header in $fileName")
                }
            }

            private fun executeAction(
                isDryRun: Boolean,
                file: File,
                content: String,
                action: (File, String) -> Unit,
            ) {
                if (!isDryRun) action(file, content)
            }

            private fun handleError(
                file: File,
                error: Exception,
                shouldFailOnMissing: Boolean,
            ) {
                logger.error("Failed to process ${file.name}: ${error.message}")
                if (shouldFailOnMissing) throw GradleException("Failed to process ${file.name}", error)
            }

            private fun writeToFile(
                file: File,
                content: String,
            ) {
                file.writeText(content, Charsets.UTF_8)
            }

            private fun hasLicenseHeader(
                content: String,
                file: File,
            ): Boolean = parseFileContent(file, content).header != null

            private fun extractExistingHeader(
                file: File,
                content: String,
            ): String? = parseFileContent(file, content).header

            private fun processTemplate(): String {
                val vars = variables.get()
                val header = header.get()
                return vars.entries.fold(header) { acc, (key, value) ->
                    acc.replace("\${$key}", value)
                }
            }

            private fun formatHeaderForFile(
                file: File,
                header: String,
            ): String {
                val style = getCommentStyleForFile(file)
                return formatHeader(header.trimIndent(), style)
            }

            private fun formatHeader(
                header: String,
                style: CommentStyle,
            ): String =
                buildString {
                    if (style.end.isBlank()) {
                        header.lineSequence().forEach { line ->
                            if (line.isBlank()) {
                                appendLine(style.start)
                            } else {
                                appendLine("${style.start} $line")
                            }
                        }
                    } else {
                        appendLine(style.start)
                        header.lineSequence().forEach { line ->
                            if (line.isBlank()) {
                                appendLine(style.middle.trimEnd())
                            } else {
                                appendLine("${style.middle}$line")
                            }
                        }
                        append(style.end)
                    }
                }

            private val commentStyleCache = mutableMapOf<String, CommentStyle>()

            private fun getCommentStyleForFile(file: File): CommentStyle {
                val extension = file.extension.lowercase()
                return commentStyleCache.getOrPut(extension) {
                    val customStyle =
                        commentStyles.get().findLast { styles ->
                            extension in styles.extensions.get()
                        }

                    customStyle?.let { style ->
                        CommentStyle(
                            start = style.start.get(),
                            middle = style.middle.get(),
                            end = style.end.get(),
                        )
                    }
                        ?: defaultStyles
                            .find { (extensions, _) ->
                                extension in extensions
                            }?.second
                        ?: defaultCommentStyle(file)
                }
            }

            private fun defaultCommentStyle(file: File): CommentStyle {
                logger.info("Using default comment style for unknown file type: ${file.name}")
                return CommentStyle("/*", " * ", " */")
            }
        }
    }

/**
 * Represents the structure of the content in a file, including the header and the main content.
 */
private data class ContentStructure(
    val header: String?,
    val content: String,
)
