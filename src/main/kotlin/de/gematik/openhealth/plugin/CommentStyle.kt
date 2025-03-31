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

data class CommentStyle(
    val start: String,
    val middle: String,
    val end: String,
) {
    init {
        require(start.isNotBlank()) { "Start comment marker cannot be blank" }
    }
}

val defaultStyles =
    listOf(
        // C-style comments
        setOf(
            "kt",
            "kts",
            "gradle",
            "java",
            "groovy",
            "js",
            "jsx",
            "ts",
            "tsx",
            "css",
            "scss",
            "less",
            "c",
            "cpp",
            "h",
            "hpp",
            "cc",
            "cxx",
            "m",
            "mm",
            "swift",
            "go",
            "scala",
            "php",
            "dart",
        ) to CommentStyle("/*", " * ", " */"),
        // HTML-style comments
        setOf(
            "html",
            "xml",
            "xhtml",
            "jsp",
            "jspx",
            "vue",
            "svelte",
            "md",
            "markdown",
            "rdoc",
            "xaml",
            "aspx",
            "cshtml",
            "htm",
            "xsl",
            "xslt",
            "svg",
            "hbs",
            "handlebars",
        ) to CommentStyle("<!--", "    ", "-->"),
        // Hash comments
        setOf(
            "py",
            "rb",
            "pl",
            "sh",
            "bash",
            "zsh",
            "fish",
            "yml",
            "yaml",
            "properties",
            "conf",
            "toml",
            "ini",
            "cfg",
            "env",
            "r",
            "rake",
            "ruby",
            "python",
            "perl",
            "tcl",
            "make",
            "makefile",
            "cmake",
        ) to CommentStyle("#", "# ", ""),
        // SQL-style comments
        setOf(
            "sql",
            "pgsql",
            "psql",
            "plsql",
            "mysql",
            "hql",
        ) to CommentStyle("--", "-- ", ""),
        // PHP complex style
        setOf("php") to CommentStyle("<?php /*", " * ", " */ ?>"),
        // Rust doc comments
        setOf("rs") to CommentStyle("//!", "//! ", ""),
        // Lisp style
        setOf("lisp", "cl", "el", "clj", "cljs", "cljc", "edn") to CommentStyle(";;", ";; ", ""),
        // Batch file style
        setOf("bat", "cmd") to CommentStyle("@REM", "@REM ", ""),
        // VB style
        setOf("vb", "bas", "vbs", "vba") to CommentStyle("'", "' ", ""),
        // Assembly style
        setOf("asm", "s", "nasm") to CommentStyle(";", "; ", ""),
        // F# style
        setOf("fs", "fsi", "fsx", "fsscript") to CommentStyle("(*", " * ", "*)"),
    )
