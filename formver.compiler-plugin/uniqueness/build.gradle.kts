plugins {
    kotlin("jvm")
    id("formver.source-layout")
}

dependencies {
    compileOnly(kotlin("compiler"))
    compileOnly(libs.kotlinx.collections.immutable)
    implementation(project(":formver.compiler-plugin:locality"))
}
