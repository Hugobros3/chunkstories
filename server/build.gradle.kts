import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    `java-library` // Needed because client uses this and needs transitive deps
    kotlin("jvm") version ("1.6.10")

    id("com.github.johnrengelman.shadow") version "5.1.0"
}

dependencies {
    api(project(":common"))

    implementation("org.fusesource.jansi:jansi:1.15")
}

application {
    mainClassName = "xyz.chunkstories.server.DedicatedServer"
}

description = "Multiplayer server"

val compileJava : JavaCompile by tasks
compileJava.apply {
    options.compilerArgs.add("-Xlint:none")
    options.encoding = "utf-8"
}

val jar: Jar by tasks
jar.apply {
    manifest {
        attributes("Implementation-Title" to "Chunk Stories Server",
                "Implementation-Version" to version)
    }
    baseName = "server"
    classifier = "bare"
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    baseName = "server"
    classifier = ""
    version = ""
}