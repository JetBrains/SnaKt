plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlin("compiler"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.7")
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDir("resources")
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}
