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
 */

package de.gematik.openhealth.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.time.LocalDate

/**
 * The `LicenseHeaderPlugin` class is a Gradle plugin that provides tasks for managing license headers in project files.
 * It defines tasks to apply, update, remove, and validate license headers.
 */
@Suppress("unused")
class LicenseHeaderPlugin : Plugin<Project> {
    /**
     * This method is called when the plugin is applied to a project.
     *
     * @param project The project to which the plugin is applied.
     */
    override fun apply(project: Project) {
        val ext = project.setupLicenseHeaderExtension()

        project.registerTask("applyLicenseHeader", ext) { applyHeaders() }
        project.registerTask("updateLicenseHeader", ext) { updateHeaders() }
        project.registerTask("removeLicenseHeader", ext) { removeHeaders() }
        project.registerTask("validateLicenseHeader", ext) { validateHeaders() }
    }

    /**
     * Extension function to register a task with the given name and action.
     *
     * @param name The name of the task to register.
     * @param extension The extension object containing configuration for the license header tasks.
     * @param action The action to perform when the task is executed.
     */
    private fun Project.registerTask(
        name: String,
        extension: LicenseHeaderExtension,
        action: LicenseHeaderExtension.TaskScope.() -> Unit,
    ) {
        tasks.register(name) { task ->
            task.doLast {
                action(extension.TaskScope(it))
            }
        }
    }
}

/**
 * Extension function to set up the `LicenseHeaderExtension` for the project,
 * which holds configuration data for the license header tasks.
 *
 * @return The configured extension object.
 */
internal fun Project.setupLicenseHeaderExtension(): LicenseHeaderExtension {
    val ext =
        extensions.create(
            "licenseHeader",
            LicenseHeaderExtension::class.java,
            objects,
            fileTree(rootDir),
        )

    ext.apply {
        variables.set(
            mapOf(
                "year" to LocalDate.now().year.toString(),
                "projectName" to name,
                "projectVersion" to version.toString(),
            ),
        )
    }

    return ext
}
