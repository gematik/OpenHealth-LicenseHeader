# OpenHealth License Header Plugin

A Gradle plugin to manage license headers in source files. Supports multiple languages and provides flexible
configuration options.

## Features

- Automatic header management (add, update, remove, validate)
- Support for multiple programming languages
- Customizable comment styles
- Variable substitution in headers
- Default configuration with override options
- Dry run mode for safe testing
- Detailed logging
- Error handling with configurable failure behavior

## Installation

Add to your `build.gradle.kts`:

```kotlin
plugins {
    id("de.gematik.openhealth.licenseheader") version "1.0.0"
}
```

## Example Configuration

Configures the license headers for the project.

```kotlin
licenseHeader {
    filesToScan.setFrom(fileTree("src") {
        include("**/*.kt")
        include("**/*.java")
    })
    header(
        """
        Copyright (c) ${'$'}{year} ${'$'}{projectName}
        
        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        """.trimIndent()
    )
    dryRun(true)
}
```

## Configuration Options

| Property        | Description                             | Default                |
|-----------------|-----------------------------------------|------------------------|
| `filesToScan`   | Files to process                        | Project root directory |
| `header`        | License header template                 | From project property  |
| `commentStyles` | File extension to comment style mapping | Predefined styles      |
| `dryRun`        | Preview changes without modifying files | `false`                |
| `failOnMissing` | Fail on missing/invalid headers         | `false`                |
| `variables`     | Template variables                      | Default variables      |

## Comment Styles

### Default Styles

- C\-style (`/* \*/`): kt, kts, gradle, java, groovy, js, jsx, ts, tsx, css, scss, less, c, cpp, h, hpp
- HTML\-style (`<!-- -->`): html, xml, xhtml, jsp, vue, svelte, md, markdown
- Hash (`\#`): py, rb, pl, sh, bash, zsh, yml, yaml, properties

### Custom Styles

```kotlin
licenseHeader {
    commentStyle("/*", " * ", " */", setOf("kt", "java"))
    commentStyle("#", "# ", "", setOf("py"))
}
```

## Template Variables

Default variables available in templates:

```kotlin
header(
    """
    Copyright (c) ${year} ${projectName} ${projectVersion}
    ...
    """.trimIndent()
)

// Override or add variables
variables(
    "year" to "2025",
    "projectName" to project.name,
    "projectVersion" to project.version,
)
```

## License

Copyright 2025 gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License.

Unless required by applicable law the software is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising from, out of or in connection with the software or the use or other dealings with the software, whether in an action of contract, tort, or otherwise.

The software is the result of research and development activities, therefore not necessarily quality assured and without the character of a liable product. For this reason, gematik does not provide any support or other user assistance (unless otherwise stated in individual cases and without justification of a legal obligation). Furthermore, there is no claim to further development and adaptation of the results to a more current state of the art.

Gematik may remove published results temporarily or permanently from the place of publication at any time without prior notice or just