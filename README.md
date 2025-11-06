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

Copyright 2025, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

See the [LICENSE](./LICENSE.md) for the specific language governing permissions and limitations under the License.

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for use. These are regularly typical conditions in connection with open source or free software. Programs described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
    1. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial portions of the Software.
    2. The software is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising from, out of or in connection with the software or the use or other dealings with the software, whether in an action of contract, tort, or otherwise.
    3. We take open source license compliance very seriously. We are always striving to achieve compliance at all times and to improve our processes. If you find any issues or have any suggestions or comments, or if you see any other ways in which we can improve, please reach out to: ospo@gematik.de
3. Please note: Parts of this code may have been generated using AI-supported technology. Please take this into account, especially when troubleshooting, for security analyses and possible adjustments.
