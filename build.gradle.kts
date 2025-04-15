import com.vanniktech.maven.publish.SonatypeHost

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
    alias(libs.plugins.vanniktech.mavenPublish)
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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.DEFAULT)

    coordinates(group.toString(), "licenseheader", version.toString())

    pom {
        name = "OpenHealth License Header Plugin"
        description = "OpenHealth License Header Plugin"
        inceptionYear = "2025"
        url = "https://github.com/gematik/OpenHealth-LicenseHeader"
        licenses {
            license {
                name = "Apache 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                name = "gematik GmbH"
                url = "https://github.com/gematik"
            }
        }
        scm {
            url = "https://github.com/gematik/OpenHealth-LicenseHeader"
            connection = "scm:git:https://github.com/gematik/OpenHealth-LicenseHeader.git"
            developerConnection = "scm:git:https://github.com/gematik/OpenHealth-LicenseHeader.git"
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
