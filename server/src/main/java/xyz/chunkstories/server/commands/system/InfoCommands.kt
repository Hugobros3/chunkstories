//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.system

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic
import xyz.chunkstories.util.VersionInfo

/** Handles /uptime, /info commands  */
class InfoCommands(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("uptime", this)
        server.pluginManager.registerCommand("info", this)
        server.pluginManager.registerCommand("help", this)
        server.pluginManager.registerCommand("plugins", this)
        server.pluginManager.registerCommand("mods", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        when {
            command.name == "uptime" -> {
                emitter.sendMessage("#00FFD0The server has been running for " + server.uptime + " seconds.")
                return true
            }
            command.name == "info" -> {
                emitter.sendMessage("#00FFD0The server's ip is " + server.publicIp)
                emitter.sendMessage("#00FFD0It's running version " + VersionInfo.versionJson.verboseVersion + " of the server software.")
                emitter.sendMessage("#00FFD0" + server.world)
                emitter.sendMessage("#00FFD0" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb used out of " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb allocated")
                return true
            }
            command.name == "help" -> {
                emitter.sendMessage("#00FFD0Avaible commands :")
                emitter.sendMessage("#00FFD0" + " /plugins")
                emitter.sendMessage("#00FFD0" + " /mods")
                emitter.sendMessage("#00FFD0" + " /list")
                emitter.sendMessage("#00FFD0" + " /info")
                emitter.sendMessage("#00FFD0" + " /uptime")
                for (availableCommand in server.pluginManager.commands()) {
                    emitter.sendMessage("#00FFD0 /" + availableCommand.name)
                }
                return true

            }
            command.name == "plugins" -> {
                val list = server.pluginManager.activePlugins().joinToString { it.name }
                emitter.sendMessage("#00FFD0${list.length} active server plugins : $list")
                return true

            }
            command.name == "mods" -> {
                val list = server.content.modsManager.currentlyLoadedMods.joinToString { it.modInfo.name }
                emitter.sendMessage("#FF0000${list.length} active server mods : $list")
                return true

            }
            else -> return false
        }
    }

}
