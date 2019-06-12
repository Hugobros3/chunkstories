import org.ajoberstar.grgit.Grgit
import java.util.Date

group = "xyz.chunkstories"
version = "Alpha1.1"
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
    id("org.ajoberstar.grgit") version "2.1.0"
    id("com.github.hierynomus.license") version "0.14.0"
}

val apiRevisionBuiltAgainst by extra { "202" }
var actualContentLocation = "core_content.zip"

val lwjglVersion by extra { "3.2.2-SNAPSHOT" }
val natives by extra { listOf("natives-windows", "natives-linux", "natives-macos")}

task("buildAll") {
    dependsOn(":client:shadowJar")
    dependsOn(":server:shadowJar")
    dependsOn(":converter:shadowJar")

    dependsOn(":launcher:createExe")
}

task("versionTxt") {
    doLast {
        val file = File("$projectDir/version.txt")
        file.writeText("""
            version: $version
            commit: ${Grgit.open().head().id}
		    buildtime: ${Date()}
        """.trimIndent())
    }
}

for(subproject in subprojects) {
    subproject.apply {
    }
}