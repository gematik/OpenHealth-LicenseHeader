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

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt) apply true
    alias(libs.plugins.gradle.publish)
}

group = project.findProperty("gematik.baseGroup") as String
version = project.findProperty("gematik.version") as String

gradlePlugin {
    website = "https://github.com/gematik/OpenHealth-LicenseHeader"
    vcsUrl = "https://github.com/gematik/OpenHealth-LicenseHeader.git"

    plugins {
        create("licenseheader") {
            id = "de.gematik.openhealth.licenseheader"
            implementationClass = "de.gematik.openhealth.plugin.LicenseHeaderPlugin"
            displayName = "OpenHealth License Plugin"
            description = "A plugin to manage license headers"
            tags = listOf("license", "licence", "header", "copyright", "gematik", "openhealth")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(gradleTestKit())
}

tasks {
    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(17)
}

val ktlint by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
    )
}

val ktlintFormat by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(
        "-F",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
    )
}

detekt {
    buildUponDefaultConfig = true
    source.from(
        files(
            fileTree(".") {
                include("**/src/**/*.kt")
                exclude("**/build/**", "**/generated/**")
            },
        ),
    )
}
