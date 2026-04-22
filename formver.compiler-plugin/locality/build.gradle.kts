plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":formver.compiler-plugin:core")) // TODO: Remove this dependency
    compileOnly(kotlin("compiler"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.8")
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
