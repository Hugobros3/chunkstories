import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    kotlin("jvm") version ("1.3.10")

    id("com.github.johnrengelman.shadow") version "4.0.1"
}

dependencies {
    implementation(project(":common"))
}

application {
    mainClassName = "xyz.chunkstories.converter.OfflineWorldConverter"
}

description = "Map converter"

val jar: Jar by tasks
jar.apply {
    manifest {
        attributes("Implementation-Title" to "Chunk Stories Server",
                "Implementation-Version" to version)
    }
    baseName = "converter"
    classifier = "bare"
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    baseName = "converter"
    classifier = ""
    version = ""
}