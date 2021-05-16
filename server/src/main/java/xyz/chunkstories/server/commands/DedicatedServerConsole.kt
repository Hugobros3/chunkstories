//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.commands

import org.fusesource.jansi.Ansi
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.util.convertToAnsi
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.util.VersionInfo
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class DedicatedServerConsole(val server: DedicatedServer) : CommandEmitter, Runnable {
    override val name: String
        get() = "[SERVER CONSOLE]"

    override fun sendMessage(message: String) {
        println(convertToAnsi("#FF00FF$message"))
    }

    // Console has ALL permissions
    override fun hasPermission(permissionNode: String): Boolean {
        return true
    }

    override fun run() {
        val br = BufferedReader(InputStreamReader(System.`in`))
        print("> ")
        while (server.keepRunning.get()) {
            try {
                // wait until we have data to complete a readLine()
                while (!br.ready() && server.keepRunning.get()) {
                    printTopScreenDebug()

                    Thread.sleep(1000L)
                }
                if (!server.keepRunning.get())
                    break
                val unparsedCommandText = br.readLine() ?: continue
                try {
                    // Parse and fire
                    var cmdName = unparsedCommandText.toLowerCase()
                    var args = arrayOf<String>()
                    if (unparsedCommandText.contains(" ")) {
                        cmdName = unparsedCommandText.substring(0, unparsedCommandText.indexOf(" "))
                        args = unparsedCommandText
                                .substring(unparsedCommandText.indexOf(" ") + 1, unparsedCommandText.length)
                                .split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    }

                    server.dispatchCommand(this, cmdName, args)

                    print("> ")
                    System.out.flush()
                } catch (e: Exception) {
                    println("error while handling command :")
                    e.printStackTrace()
                }

            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                println("ConsoleInputReadTask() cancelled")
                break
            }

        }
        try {
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun printTopScreenDebug() {
        var txt = "" + Ansi.ansi().fg(Ansi.Color.BLACK).bg(Ansi.Color.CYAN)

        val ec = server.world.entities_.size

        val maxRam = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val freeRam = Runtime.getRuntime().freeMemory() / (1024 * 1024)
        val usedRam = maxRam - freeRam

        txt += "ChunkStories server " + VersionInfo.versionJson.verboseVersion
        // txt += " | fps:" + server.world.gameLogic.simulationFps
        txt += " | ent:$ec"
        txt += " | players:" + server.connectionsManager.playersNumber + "/" + server.connectionsManager.maxClients
        txt += (" | lc:" + server.world.regionsManager.regionsList.count() + " ls:" + server.world.heightmapsManager.all().count())
        txt += " | ram:$usedRam/$maxRam"
        txt += " | " + server.tasks.toShortString()
        txt += " | ioq:" + server.world.ioThread.size

        txt += Ansi.ansi().bg(Ansi.Color.BLACK).fg(Ansi.Color.WHITE)

        System.out.print(Ansi.ansi().saveCursorPosition().cursor(0, 0).eraseLine().fg(Ansi.Color.RED).toString() + txt + Ansi.ansi().restoreCursorPosition())
        System.out.flush()
    }
}