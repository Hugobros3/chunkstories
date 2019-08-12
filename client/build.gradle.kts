import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    kotlin("jvm") version ("1.3.10")

    id("com.github.johnrengelman.shadow") version "5.1.0"
}

dependencies {
    api(project(":server"))

    // Graphviz (debug graphs)
    implementation("guru.nidi:graphviz-java:0.8.0")

    // SPIRVCross bindings
    val spirvCrossVersion by extra { "0.5.0-1.1.85" }
    implementation("graphics.scenery:spirvcrossj:$spirvCrossVersion")
    for(native in listOf("natives-windows", "natives-linux", "natives-macos"))
        runtime("graphics.scenery:spirvcrossj:$spirvCrossVersion:$native")

    // LWJGL3 bindings
    val lwjglVersion = rootProject.extra.get("lwjglVersion")

    // JVM modules
    val lwjglModules = listOf("glfw", "openal", "opengl", "vulkan", "stb", "tinyfd")
    for(module in lwjglModules) {
        implementation("org.lwjgl:lwjgl-$module:${lwjglVersion}")
    }

    // Modules that needs native libs
    val lwjglNativeModules = listOf("glfw", "openal", "opengl", "stb", "tinyfd")
    for(module in lwjglNativeModules) {
        for(native in listOf("natives-windows", "natives-linux", "natives-macos"))
            runtime("org.lwjgl:lwjgl-$module:${lwjglVersion}:$native")
    }
}

application {
    mainClassName = "xyz.chunkstories.client.ClientImplementation"
}

description = "Multiplatform PC Client using LWJGL3"

val jar: Jar by tasks
jar.apply {
    manifest {
        attributes("Implementation-Title" to "Chunk Stories Client",
                "Implementation-Version" to version)
    }
    baseName = "client"
    classifier = "bare"
}

tasks {
    processResources {
        dependsOn(rootProject.tasks["versionTxt"])
    }
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    baseName = "chunkstories"
    classifier = ""
    version = ""
}