plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":formver.compiler-plugin:analysis"))
    compileOnly(project(":formver.compiler-plugin:core")) // TODO: Remove this dependency
    compileOnly(kotlin("compiler"))
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
