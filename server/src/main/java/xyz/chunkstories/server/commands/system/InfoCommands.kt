//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.system

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.gameName
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.commands.AbstractHostCommandHandler
import xyz.chunkstories.util.VersionInfo

/** Handles /uptime, /info commands  */
class InfoCommands(host: Host) : AbstractHostCommandHandler(host) {

    init {
        if (host is DedicatedServer)
            this.host.pluginManager.registerCommand("uptime", this)

        this.host.pluginManager.registerCommand("info", this)
        this.host.pluginManager.registerCommand("help", this)
        this.host.pluginManager.registerCommand("plugins", this)
        this.host.pluginManager.registerCommand("mods", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        when {
            command.name == "uptime" && host is DedicatedServer -> {
                emitter.sendMessage("#00FFD0The server has been running for " + host.uptime + " seconds.")
                return true
            }
            command.name == "info" -> {
                emitter.sendMessage("#00FFD0$gameName server version " + VersionInfo.versionJson.verboseVersion)
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
                for (availableCommand in host.pluginManager.commands) {
                    emitter.sendMessage("#00FFD0 /" + availableCommand.name)
                }
                return true

            }
            command.name == "plugins" -> {
                val list = host.pluginManager.activePlugins.joinToString { it.name }
                emitter.sendMessage("#00FFD0${list.length} active server plugins : $list")
                return true

            }
            command.name == "mods" -> {
                val list = host.content.modsManager.currentlyLoadedMods.joinToString { it.modInfo.name }
                emitter.sendMessage("#FF0000${list.length} active server mods : $list")
                return true

            }
            else -> return false
        }
    }

}
