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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.impldep.junit.framework.TestCase
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LicenseHeaderExtensionTest {
    private lateinit var project: Project
    private lateinit var task: Task
    private lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        task = project.tasks.create("dummy")
        tempDir = createTempDirectory("license-header-test").toFile()
    }

    @AfterEach
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    private fun createExtension(): LicenseHeaderExtension =
        project.setupLicenseHeaderExtension().apply {
            filesToScan.setFrom(project.fileTree(tempDir))
            header.set("Copyright notice")
        }

    private fun LicenseHeaderExtension.scope() = TaskScope(task)

    @Test
    fun `applies header with correct comment style for kotlin file`() {
        val file = File(tempDir, "test.kt")
        file.writeText("package test\n\nclass Test")
        val ext = createExtension()

        ext.scope().applyHeaders()

        val content = file.readText()
        TestCase.assertTrue(content.startsWith("/*\n * Copyright notice\n */"))
    }

    @Test
    fun `updates existing header in kotlin file`() {
        val file = File(tempDir, "test.kt")
        file.writeText("/*\n * Old header\n */\n\nclass Test")
        val ext = createExtension()

        ext.scope().updateHeaders()

        val content = file.readText()
        TestCase.assertTrue(content.startsWith("/*\n * Copyright notice\n */"))
        TestCase.assertTrue(content.contains("class Test"))
    }

    @Test
    fun `removes header from kotlin file`() {
        val file = File(tempDir, "test.kt")
        file.writeText("/*\n * Copyright notice\n */\n\nclass Test")
        val ext = createExtension()

        ext.scope().removeHeaders()

        val content = file.readText()
        TestCase.assertFalse(content.contains("Copyright notice"))
        assertEquals("class Test", content.trim())
        TestCase.assertFalse(content.startsWith("\n"))
    }

    @Test
    fun `validates headers and fails on missing`() {
        val file = File(tempDir, "test.kt")
        file.writeText("class Test")
        val ext = createExtension()
        ext.failOnMissing.set(true)

        assertFailsWith<GradleException> { ext.scope().validateHeaders() }
    }

    @Test
    fun `respects dry run mode`() {
        val file = File(tempDir, "test.kt")
        val originalContent = "class Test"
        file.writeText(originalContent)
        val ext = createExtension()
        ext.dryRun.set(true)

        ext.scope().applyHeaders()

        assertEquals(originalContent, file.readText())
    }

    @Test
    fun `processes template variables`() {
        val file = File(tempDir, "test.kt")
        file.writeText("class Test")
        val ext = createExtension()
        ext.header.set("Copyright (c) \${year} \${projectName}")

        ext.scope().applyHeaders()

        val content = file.readText()
        println("cp: $content")
        assertTrue(content.contains("Copyright (c) ${ext.variables.get()["year"]}"))
        assertTrue(content.contains(project.name))
    }

    @Test
    fun `handles different comment styles`() {
        data class TestCase(
            val ext: String,
            val expectedStart: String,
            val expectedMiddle: String,
            val expectedEnd: String,
        )

        val testCases =
            listOf(
                TestCase("kt", "/*", " * ", " */"),
                TestCase("py", "#", "# ", ""),
                TestCase("html", "<!--", "    ", "-->"),
            )

        testCases.forEach { (ext, expectedStart, expectedMiddle, expectedEnd) ->
            val testProject = ProjectBuilder.builder().build()
            val testDir = createTempDirectory("license-header-test-$ext").toFile()

            try {
                val file = File(testDir, "test.$ext")
                file.writeText("content")

                val ext =
                    testProject.setupLicenseHeaderExtension().apply {
                        header("Copyright notice")
                        filesToScan.setFrom(project.fileTree(testDir.path))

                        commentStyle("/*", " * ", " */", setOf("kt", "java"))
                        commentStyle("#", "# ", "", setOf("py"))
                        commentStyle("<!--", "    ", "-->", setOf("html"))
                    }

                ext.scope().applyHeaders()

                val content = file.readText()
                assertTrue(
                    content.startsWith(expectedStart),
                    "File with extension $ext should start with $expectedStart",
                )
                assertTrue(content.contains(expectedMiddle), "File with extension $ext should contain $expectedMiddle")
                if (expectedEnd.isNotEmpty()) {
                    assertTrue(content.contains(expectedEnd), "File with extension $ext should contain $expectedEnd")
                }
            } finally {
                testDir.deleteRecursively()
            }
        }
    }

    @Test
    fun `handles files with different encodings`() {
        val file = File(tempDir, "test.kt")
        val content = "class Test // with special chars: äöü"
        file.writeText(content, Charsets.UTF_8)
        val ext = createExtension()

        ext.scope().applyHeaders()

        val result = file.readText(Charsets.UTF_8)
        TestCase.assertTrue(result.contains("äöü"))
    }

    @Test
    fun `handles malformed files gracefully`() {
        val file = File(tempDir, "test.kt")
        file.writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
        val ext = createExtension()
        ext.failOnMissing.set(false)

        ext.scope().applyHeaders() // Should not throw exception
    }

    @Test
    fun `custom comment styles take precedence over defaults`() {
        val file = File(tempDir, "test.kt")
        val testProject = ProjectBuilder.builder().build()

        file.writeText("class Test")
        val ext =
            testProject.setupLicenseHeaderExtension().apply {
                header.set("Copyright notice") // Set the license header
                filesToScan.setFrom(project.fileTree(file))
                commentStyle {
                    it.start.set("//")
                    it.middle.set("// ")
                    it.end.set("")
                    it.extensions.set(setOf("kt"))
                }
            }

        ext.scope().applyHeaders()

        val content = file.readText()
        println("cp: $content")

        TestCase.assertTrue(content.startsWith("// Copyright notice"))
    }

    @Test
    fun `handles empty files`() {
        val file = File(tempDir, "test.kt")
        file.writeText("")
        val ext = createExtension()

        ext.scope().applyHeaders()

        val content = file.readText()
        TestCase.assertTrue(content.startsWith("/*\n * Copyright notice\n */"))
    }

    @Test
    fun `validates headers without failing`() {
        val file = File(tempDir, "test.kt")
        file.writeText("class Test")
        val ext = createExtension()
        ext.failOnMissing.set(false)
        ext.scope().validateHeaders()
    }

    @Test
    fun `extension properties can't be set twice`() {
        val ext = createExtension()
        ext.failOnMissing(false)
        assertFails { ext.failOnMissing(false) }

        ext.header("Initial header")
        assertFails { ext.header("Second header") }

        ext.dryRun(true)
        assertFails { ext.dryRun(false) }

        ext.variables(mapOf("key" to "value"))
        assertFails { ext.variables(mapOf("anotherKey" to "anotherValue")) }
    }
}
