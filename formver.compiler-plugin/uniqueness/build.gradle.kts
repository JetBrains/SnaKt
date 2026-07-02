plugins {
    kotlin("jvm")
    id("formver.source-layout")
}

dependencies {
    compileOnly(kotlin("compiler"))
    implementation(project(":formver.common"))
}
