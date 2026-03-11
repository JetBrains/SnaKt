# Installation

SnaKt is not published to Maven Central. You must build it from source and publish to your local Maven repository.

---

## Prerequisites

### JDK 21

SnaKt requires Java 21.

```bash
# macOS with SDKMAN
sdk install java 21-amzn

# Ubuntu
sudo apt install openjdk-21-jdk
```

Verify: `java -version` must print `21.x.x`.

### Z3 v4.8.7

SnaKt requires **exactly version 4.8.7** of the Z3 SMT solver. Other versions are not supported.

1. Download the archive for your OS/architecture from [GitHub Releases](https://github.com/Z3Prover/z3/releases/tag/z3-4.8.7).
2. Install the binary and set the environment variable:

```bash
# Example for Linux x64
tar xzf z3-4.8.7-x64-glibc-2.31.tar.gz
export Z3_EXE=/usr/local/bin/z3
sudo cp z3-4.8.7-x64-glibc-2.31/bin/z3 $Z3_EXE

# Persist across shells
echo 'export Z3_EXE=/usr/local/bin/z3' >> ~/.profile
source ~/.profile
```

3. Verify:

```bash
$Z3_EXE --version
# Z3 version 4.8.7 - 64 bit
```

!!! warning
    `Z3_EXE` must be set in every shell where you run tests or compilation, including the shell used by your IDE. On macOS you may also need to set it in `~/.zshrc` or `~/.bash_profile`.

### Gradle

The project uses the Gradle Wrapper (`./gradlew`). No separate Gradle installation is needed.

---

## Build from source

```bash
git clone https://github.com/jesyspa/SnaKt.git
cd SnaKt
./gradlew publishToMavenLocal
```

This puts the built artifacts into `~/.m2/repository/`.

!!! note
    On the first run Gradle downloads Silicon and its dependencies from a JetBrains Space Maven repository. This may take a few minutes.

---

## Use in your project

After publishing to local Maven, add the plugin to your project.

**`settings.gradle.kts`:**

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
```

**`build.gradle.kts`:**

```kotlin
plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.kotlin.formver") version "0.1.0-SNAPSHOT"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin.formver:formver.annotations:0.1.0-SNAPSHOT")
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    // The Kotlin daemon needs extra stack space for deep FIR traversal
    kotlinDaemonJvmArgs = listOf("-Xss30m")
}
```

See [jesyspa/snakt-usage-example](https://github.com/jesyspa/snakt-usage-example) for a complete minimal project.

---

## Command-line use

To invoke the plugin directly without Gradle:

```bash
kotlinc -language-version 2.0 \
    -Xplugin=path/to/formver.compiler-plugin.jar \
    myfile.kt
```

Pass options with `-P plugin:org.jetbrains.kotlin.formver:OPTION=VALUE`. See [Configuration](configuration.md) for all available options.
