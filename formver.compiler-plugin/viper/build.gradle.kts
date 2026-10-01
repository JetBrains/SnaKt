plugins {
    kotlin("jvm")
    id("formver.source-layout")
}

dependencies {
    implementation(libs.viper.silicon)
    testImplementation(kotlin("test-junit5"))
}

sourceSets.test {
    java.setSrcDirs(listOf("test"))
}

tasks.test {
    useJUnitPlatform()
}
