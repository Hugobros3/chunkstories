//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.commands

import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.plugin.commands.ServerConsole
import xyz.chunkstories.api.util.convertToAnsi
import xyz.chunkstories.server.DedicatedServer

/**
 * Handles basic commands and forwards not-so-basic commands to plugins Can send
 * command itself
 */
class DedicatedServerConsole(override val server: DedicatedServer) : ServerConsole {
    fun dispatchCommand(emitter: CommandEmitter, command: String, arguments: Array<String>): Boolean {
        server.logger().info("[" + emitter.name + "] " + "Entered command : " + command)
        try {
            if (server.pluginManager.dispatchCommand(emitter, command, arguments)) return true
        } catch (e: Exception) {
            emitter.sendMessage("An exception happened while handling your command : " + e.localizedMessage)
            e.printStackTrace()
        }
        return false
    }

    override val name: String
        get() = "[SERVER CONSOLE]"

    override fun sendMessage(msg: String) {
        println(convertToAnsi("#FF00FF$msg"))
    }

    override fun hasPermission(permissionNode: String): Boolean {
        // Console has ALL permissions
        return true
    }
}