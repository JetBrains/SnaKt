plugins {
    kotlin("jvm")
    id("maven-publish")
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("compiler"))
    api(kotlin("compiler-internal-test-framework"))
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
