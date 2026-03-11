import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("com.github.gmazzo.buildconfig") version "5.6.5"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3" apply false
    id("com.gradle.plugin-publish") version "1.3.1" apply false
    id("org.jetbrains.dokka") version "2.1.0"
}

allprojects {
    group = "org.jetbrains.kotlin.formver"
    version = "0.1.0-SNAPSHOT"

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

// Aggregate API docs for all source modules into docs/api/ (Dokka V2).
// formver.compiler-plugin is a parent-aggregator with no source files and is intentionally omitted.
dependencies {
    dokka(project(":formver.annotations"))
    dokka(project(":formver.common"))
    dokka(project(":formver.gradle-plugin"))
    dokka(project(":formver.compiler-plugin:cli"))
    dokka(project(":formver.compiler-plugin:core"))
    dokka(project(":formver.compiler-plugin:plugin"))
    dokka(project(":formver.compiler-plugin:uniqueness"))
    dokka(project(":formver.compiler-plugin:viper"))
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.projectDirectory.dir("docs/api/dokka"))
    }
}
