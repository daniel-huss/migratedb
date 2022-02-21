/*
 * Copyright 2022 The MigrateDB contributors
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
import java.time.Instant

static String sanitize(Object it) { (it?.toString() ?: "").replaceAll(/[^\w_.:-]/, "") }

static File findLicenseHeaderFile(Object startDir) {
    File dir = startDir as File
    File licenseFile
    do {
        licenseFile = new File(dir, "LICENSE-HEADER.txt")
        dir = dir.parentFile
    } while (!licenseFile.exists() && dir != null && new File(dir, "pom.xml").exists())
    return licenseFile
}

String lineSeparator = "\n"
String sourceEncoding = project.properties["project.build.sourceEncoding"]?.toString() ?: "UTF-8"
String targetPackage = "migratedb.core.internal.info"
String licenseHeader = findLicenseHeaderFile(project.basedir)
        .readLines(sourceEncoding)
        .collect { "* $it" }
        .with { "/*$lineSeparator${it.join(lineSeparator)}$lineSeparator */" }
String sourceCode = """
$licenseHeader
package $targetPackage;

import java.time.Instant;

public final class BuildInfo {
    public static final Instant TIMESTAMP = Instant.parse("${sanitize(project.properties["project.build.outputTimestamp"] ?: Instant.now())}");
    public static final String VERSION = "${sanitize(project.version)}";
}
""".trim()

File sourceRoot = new File(project.properties["project.build.generatedSources.directory"]?.toString() ?: ".")
project.addCompileSourceRoot(sourceRoot.absolutePath)

File outFile = targetPackage.tokenize(".")
        .inject(sourceRoot) { p, c -> new File(p, c) }
        .with { new File(it, "BuildInfo.java") }
outFile.parentFile.mkdirs()
outFile.write(sourceCode.trim(), sourceEncoding)

