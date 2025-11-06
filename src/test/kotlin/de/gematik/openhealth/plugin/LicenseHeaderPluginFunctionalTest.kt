/*
 * Copyright 2025, gematik GmbH
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
 *
 * *******
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */

package de.gematik.openhealth.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LicenseHeaderPluginFunctionalTest {
    @TempDir
    lateinit var testProjectDir: File
    private lateinit var buildFile: File
    private lateinit var sourceFile: File

    @BeforeTest
    fun setup() {
        testProjectDir.deleteRecursively()
        testProjectDir.mkdirs()

        File(testProjectDir, "settings.gradle.kts").apply {
            writeText(
                """
                rootProject.name = "test-project"
                """.trimIndent(),
            )
        }

        buildFile =
            File(testProjectDir, "build.gradle.kts").apply {
                writeText(
                    """
                    plugins {
                        id("de.gematik.openhealth.licenseheader")
                        id("base")  // Add base plugin for clean task
                    }
                    
                    licenseHeader {
                        filesToScan.setFrom(fileTree("src"))
                        header(""${'"'}
                            Copyright (c) 2025 gematik GmbH
                            
                            You may not use this file except in compliance with the License.
                            You may obtain a copy of the License at
                            
                            http://www.apache.org/licenses/LICENSE-2.0
                            
                            Unless required by applicable law or agreed to in writing, software
                            distributed under the License is distributed on an "AS IS" BASIS,
                            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                            See the License for the specific language governing permissions and
                            limitations under the License.
                        ""${'"'}.trimMargin())
                        failOnMissing(true)
                    }
                    """.trimIndent(),
                )
            }

        File(testProjectDir, "src/main/kotlin").apply {
            mkdirs()
            sourceFile =
                File(this, "Test.kt").apply {
                    parentFile.mkdirs()
                    writeText(
                        """
                        package test
                        
                        class Test
                        """.trimIndent(),
                    )
                }
        }
    }

    @Test
    fun `apply license header to kotlin file`() {
        val result = runTask("applyLicenseHeader")

        assertEquals(TaskOutcome.SUCCESS, result.task(":applyLicenseHeader")?.outcome)
        assertTrue(sourceFile.readText().contains("Copyright (c)"))
    }

    @Test
    fun `update license header in kotlin file`() {
        runTask("applyLicenseHeader")
        buildFile.writeText(buildFile.readText().replace("gematik GmbH", "gematik"))
        val result = runTask("updateLicenseHeader")

        assertEquals(TaskOutcome.SUCCESS, result.task(":updateLicenseHeader")?.outcome)
        assertTrue(sourceFile.readText().contains("Copyright (c)"))
        assertTrue(sourceFile.readText().contains("gematik"))
        assertFalse(sourceFile.readText().contains("gematik GmbH"))
    }

    @Test
    fun `remove license header from kotlin file`() {
        runTask("applyLicenseHeader")
        val result = runTask("removeLicenseHeader")

        assertEquals(TaskOutcome.SUCCESS, result.task(":removeLicenseHeader")?.outcome)
        assertFalse(sourceFile.readText().contains("Copyright"))
    }

    @Test
    fun `validate license header in kotlin file`() {
        runTask("applyLicenseHeader")
        val result = runTask("validateLicenseHeader")
        assertEquals(TaskOutcome.SUCCESS, result.task(":validateLicenseHeader")?.outcome)
    }

    @Test
    fun `validate fails when header is missing`() {
        val result = runTask("validateLicenseHeader", shouldFail = true)
        assertEquals(TaskOutcome.FAILED, result.task(":validateLicenseHeader")?.outcome)
    }

    @Test
    fun `handles multiple file types`() {
        File(testProjectDir, "src/main/java").mkdirs()
        File(testProjectDir, "src/main/resources").mkdirs()

        val javaFile =
            File(testProjectDir, "src/main/java/Test.java").apply {
                writeText(
                    """
                    package test;
                    
                    public class Test {}
                    """.trimIndent(),
                )
            }

        val propertiesFile =
            File(testProjectDir, "src/main/resources/test.properties").apply {
                writeText("key=value")
            }

        val result = runTask("applyLicenseHeader")

        assertEquals(TaskOutcome.SUCCESS, result.task(":applyLicenseHeader")?.outcome)
        assertTrue(javaFile.readText().contains("Copyright"))
        assertTrue(propertiesFile.readText().contains("Copyright"))
    }

    private fun runTask(
        vararg arguments: String,
        shouldFail: Boolean = false,
    ): BuildResult {
        val runner =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(*arguments, "--stacktrace")
                .withPluginClasspath()
                .withDebug(true)
                .forwardOutput()

        return try {
            if (shouldFail) runner.buildAndFail() else runner.build()
        } catch (_: Exception) {
            File(testProjectDir, ".gradle").deleteRecursively()
            if (shouldFail) runner.buildAndFail() else runner.build()
        }
    }
}
