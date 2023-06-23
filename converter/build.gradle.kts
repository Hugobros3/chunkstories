import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    kotlin("jvm") version ("1.8.10")
    id("com.github.johnrengelman.shadow") version "7.0.0"
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
                "Implementation-Version" to archiveVersion)
    }
    archiveBaseName.set("converter")
    archiveClassifier.set("bare")
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    archiveBaseName.set("converter")
    archiveClassifier.set("")
    archiveVersion.set("")
}