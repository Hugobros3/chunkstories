import java.util.Date

group = "xyz.chunkstories"
version = "1.1.0"
val verboseVersion = "cleanup-branch"

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()

        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
    }

    group = rootProject.group
    version = rootProject.version
}

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
}

plugins {
    id("com.palantir.git-version") version "3.0.0"
}

val apiRevisionBuiltAgainst by extra { "2.0.4" }
var actualContentLocation = "core_content.zip"

val lwjglVersion by extra { "3.3.2" }
val lwjglNatives by extra { listOf("natives-windows", "natives-linux", "natives-macos")}

task("buildAll") {
    dependsOn(":api:publishToMavenLocal")
    dependsOn(":enklume:publishToMavenLocal")
    dependsOn(":client:shadowJar")
    dependsOn(":server:shadowJar")
    dependsOn(":converter:shadowJar")

    dependsOn(":launcher:createExe")
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra

task("versionTxt") {
    doLast {
        val details = versionDetails()

        val file = File("${project.rootDir}/version.json")
        file.writeText("""
            {
                "version": "$version",
                "verboseVersion": "$verboseVersion",
                "commit": "${details.gitHashFull}",
		        "buildtime": "${Date()}"
            }
        """.trimIndent())
    }
}