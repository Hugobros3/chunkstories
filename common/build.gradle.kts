plugins {
    java
    `java-library`
    kotlin("jvm") version ("1.8.10")
}

val lwjglVersion: String by rootProject.extra
val lwjglNatives: List<String> by rootProject.extra

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))

    // We obviously depend on the project's API !
    // api("xyz.chunkstories:api:${rootProject.extra.get("apiRevisionBuiltAgainst")}")
    api(project(":api"))

    api("org.lwjgl:lwjgl:${rootProject.extra.get("lwjglVersion")}")
    for (natives in lwjglNatives)
        api("org.lwjgl:lwjgl:${rootProject.extra.get("lwjglVersion")}:${natives}")

    val lwjglModules = listOf("assimp", "stb", "tinyfd")
    for(module in lwjglModules) {
        implementation("org.lwjgl:lwjgl-$module:${lwjglVersion}")
    }

    // Modules that needs native libs
    val lwjglNativeModules = listOf("assimp", "stb", "tinyfd")
    for(module in lwjglNativeModules) {
        for(native in lwjglNatives)
            runtimeOnly("org.lwjgl:lwjgl-$module:${lwjglVersion}:$native")
    }

    // Some high-performance collections we like
    api("com.carrotsearch:hppc:0.7.2")

    // Sound decoding
    // TODO client-only isn't it
    api("com.googlecode.soundlibs:jorbis:0.0.17.4")

    // PNG loading
    // TODO client-only isn't it
    // TODO sort out the duplication with ImageIO.read
    api("org.l33tlabs.twl:pngdecoder:1.0")

    // Assimp & friends
    api("io.github.kotlin-graphics:glm:0.9.9.1-11")
    //implementation("com.github.kotlin-graphics:assimp:4.0")

    // JSON stuff
    api("com.google.code.gson:gson:2.8.5")
    implementation("org.hjson:hjson:1.0.0")

    // Compression fun times
    api("net.jpountz.lz4:lz4:1.3.0")

    // Networking
    // Wait we don't actually use netty for now
    // implementation("io.netty:netty-all:4.1.18.Final")

    // Fancy concurrency
    api("com.googlecode.concurrent-locks:concurrent-locks:1.0.0")

    // Logging
    implementation("ch.qos.logback:logback-core:1.0.13")
    implementation("ch.qos.logback:logback-classic:1.0.13")
    implementation("org.slf4j:slf4j-api:1.7.25")

    // Obligatory JUnit
    testApi("junit:junit:4.12")
}

tasks {
    test {
        workingDir = rootProject.projectDir

        maxHeapSize = "2G"

        doFirst {
            //TODO Make this configurable
            systemProperty("coreContentLocation", "../chunkstories-core/res")
        }
    }
}
