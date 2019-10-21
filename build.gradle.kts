import org.ajoberstar.grgit.Grgit
import java.util.Date

group = "xyz.chunkstories"
version = "0.1.1"
val verboseVersion = "Alpha 1.1"
description = "A voxel game engine"

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()

        //TODO Fix HTTPS there
        maven("http://maven.xol.io/repository/public/")
        maven("https://jitpack.io")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    group = rootProject.group
    version = rootProject.version
}

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

plugins {
    id("org.ajoberstar.grgit") version "3.1.1"
    id("com.github.hierynomus.license") version "0.14.0"
}

val apiRevisionBuiltAgainst by extra { "203" }
var actualContentLocation = "core_content.zip"

val lwjglVersion by extra { "3.2.2-SNAPSHOT" }
val natives by extra { listOf("natives-windows", "natives-linux", "natives-macos")}

task("buildAll") {
    dependsOn(":client:shadowJar")
    dependsOn(":server:shadowJar")
    dependsOn(":converter:shadowJar")

    dependsOn(":launcher:createExe")
}

val git = Grgit.open(mapOf("currentDir" to project.rootDir))

task("versionTxt") {
    doLast {
        val file = File("${project.rootDir}/version.json")
        file.writeText("""
            {
                "version": "$version",
                "verboseVersion": "$verboseVersion",
                "commit": "${git.head().id}",
		        "buildtime": "${Date()}"
            }
        """.trimIndent())
    }
}