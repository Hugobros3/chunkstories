import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        mavenCentral()
        maven("https://clojars.org/repo/")
    }
}

plugins {
    java
    application
    kotlin("jvm") version ("1.3.10")

    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("edu.sc.seis.launch4j") version "2.4.6"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("junit:junit:4.12")
}

application {
    mainClassName = "xyz.chunkstories.updater.ChunkStoriesLauncher"
}

description = "Game launcher & updater"
version = "1.2.1"

val jar: Jar by tasks
jar.apply {
    manifest {
        attributes("Implementation-Title" to "Chunk Stories Launcher",
                "Implementation-Version" to version)
    }
    baseName = "launcher"
    classifier = "bare"
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    baseName = "launcher"
    classifier = ""
    version = ""
}

launch4j {
    mainClassName = project.application.mainClassName
    jar = "../libs/launcher.jar"
    icon = "${projectDir}/favicon.ico"
}