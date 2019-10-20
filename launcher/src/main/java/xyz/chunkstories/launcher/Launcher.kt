package xyz.chunkstories.launcher

import java.io.File
import javax.swing.JOptionPane
import kotlin.system.exitProcess

fun main(arguments: Array<String>) {
    val jvmInfo = checkVersion()

    val preLaunchOptions = loadOrDefaultPreLaunchOptions()

    val executable = File("chunkstories.jar")
    if(!executable.exists()) {
        fail("Couldn't find game executable (chunkstories.jar), double check the launcher is placed in the same directory as the game data !")
    }

    @Suppress("SpellCheckingInspection")
    val process = Runtime.getRuntime().exec("java -Xmx{${preLaunchOptions.dedicatedRam}}M -jar chunkstories.jar", null, File("."))
    process.waitFor()
}

fun warn(message: String) {
    System.err.println("Warning, the game might malfunction:")
    System.err.println(message)
    JOptionPane.showMessageDialog(null, message, "Warning, the game might malfunction", JOptionPane.WARNING_MESSAGE)
}

fun fail(message: String) {
    System.err.println("Failed to launch game:")
    System.err.println(message)
    JOptionPane.showMessageDialog(null, message, "Failed to launch game", JOptionPane.ERROR_MESSAGE)
    exitProcess(-1)
}