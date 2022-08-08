---
layout: code
pill: codeSetup
subtitle: Dev Environment Setup - Code
---

# Code: Dev Environment Setup

To contribute to MigrateDB you will need to set up your development environment so that you can build and run MigrateDB.

For this you will need to set up Git, a JDK, Maven and your IDE.

## Git

MigrateDB uses Git for version control. Download the latest version from the [Git homepage](https://git-scm.com/).

Make sure the directory containing the binaries has been added the `PATH`. If you downloaded an installer this
should have been taken care of for you.

## JDK

MigrateDB depends on JDK 11 or later.

## Maven

MigrateDB is built with Maven 3. So grab the latest version from
the [Apache website](http://maven.apache.org/download.html).

After the installation is complete

- set up an environment variable called `M2_HOME` that points to your Maven installation directory
- add the `bin` directory under `M2_HOME` to the `PATH`

## IDE

We use IntelliJ for development. You can grab the latest version from
the [JetBrains website](http://www.jetbrains.com/idea/).

Eclipse should be fine too. However Eclipse has different
defaults for code formatting and import reordering. Keep this in mind so merge conflicts can be reduced to a
minimum.

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/contribute/code/submit">Submit your changes ➡️</a>
</p>
