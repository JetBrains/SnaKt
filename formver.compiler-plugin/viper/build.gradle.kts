plugins {
    kotlin("jvm")
}

dependencies {
    implementation(ViperVersions.silicon)
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDir("resources")
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(emptyList<String>())
    }
}
