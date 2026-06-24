/*
 * Copyright 2022-2026 The MigrateDB contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This script generates the Spring Boot configuration properties documentation for use in README.adoc

import HtmlEscapers
@Grapes(
    @Grab(group = 'com.google.guava', module = 'guava', version = '32.1.2-jre')
)
import groovy.json.JsonSlurper

import java.nio.file.Files
import java.nio.file.Paths

import static java.nio.charset.StandardCharsets.UTF_8

def runAndGetStdout = { String... command ->
    System.err.println("Running ${command.join(' ')}")
    var stdout = new ByteArrayOutputStream()
    var stderr = new ByteArrayOutputStream()
    var process = command.execute()
    process.waitForProcessOutput(stdout, stderr)
    if (process.exitValue() != 0) {
        throw new Exception(stderr.toString(UTF_8).trim())
    }
    return stdout
}

var userId = runAndGetStdout('id', '-u').toString(UTF_8).trim()
var groupId = runAndGetStdout('id', '-g').toString(UTF_8).trim()
var targetDir = Paths.get("target").toAbsolutePath()

def docCommentToAsciiDoc = { String docComment ->
    var html = docComment.replaceAll(/(?s)\{@(?:code|link) ([^}]+)}/) {
        '<code>' + HtmlEscapers.htmlEscaper().escape(it[1]) + '</code>'
    }
    var inputFile = Files.createTempFile(targetDir, 'conv', '.tmp')
    Files.writeString(inputFile, html)
    try {
        var stdout = runAndGetStdout('docker', 'run', '--rm', '--volume', "$targetDir:/data", '--user', "$userId:$groupId",
            'pandoc/latex:latest-alpine', inputFile.fileName.toString(), '-f', 'html', '-t', 'asciidoc', '-o', '-')
        return stdout.toString(UTF_8).trim()
    } finally {
        Files.deleteIfExists(inputFile)
    }
}

def runConversion(String dir) {
    JsonSlurper parser = new JsonSlurper()
    def metadata = parser.parseText(Files.readString(Paths.get("$dir/target/classes/META-INF/spring-configuration-metadata.json")))

    def asciidoc = new StringBuilder(4096)

    metadata['properties'].each { prop ->
        var name = prop.name
        var desc = docCommentToAsciiDoc(prop.description)
        var type = prop.type
        asciidoc.append("$name ($type)::\n")
        desc.readLines().each {
            // '__' seems to pass through pandoc even though it is an inline formatting instruction
            var line = it.replaceAll(/\w+/) {
                it[0].contains('__') ? ('+' + it[0] + '+') : it[0]
            }
            // Replacing empty lines with asciidoc list item continuations
            asciidoc.append(line.trim().isEmpty() ? '+' : it).append('\n')
        }
    }

    println asciidoc
}

if (args.length != 1) {
    println("Usage: ")
}

runConversion()
