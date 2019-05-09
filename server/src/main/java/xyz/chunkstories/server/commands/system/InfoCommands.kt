//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.system

import xyz.chunkstories.api.content.mods.Mod
import xyz.chunkstories.api.plugin.ChunkStoriesPlugin
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

    override fun handleCommand(emitter: CommandEmitter, cmd: Command, arguments: Array<String>): Boolean {
        if (cmd.name == "uptime") {
            emitter.sendMessage("#00FFD0The server has been running for " + server.uptime + " seconds.")
            return true
        } else if (cmd.name == "info") {
            emitter.sendMessage("#00FFD0The server's ip is " + server.publicIp)
            emitter.sendMessage("#00FFD0It's running version " + VersionInfo.version + " of the server software.")
            emitter.sendMessage("#00FFD0" + server.world)
            emitter.sendMessage("#00FFD0" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb used out of "
                    + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb allocated")
            return true
        } else if (cmd.name == "help") {
            emitter.sendMessage("#00FFD0Avaible commands :")
            emitter.sendMessage("#00FFD0" + " /plugins")
            emitter.sendMessage("#00FFD0" + " /mods")
            emitter.sendMessage("#00FFD0" + " /list")
            emitter.sendMessage("#00FFD0" + " /info")
            emitter.sendMessage("#00FFD0" + " /uptime")
            for (command in server.pluginManager.commands()) {
                emitter.sendMessage("#00FFD0 /" + command.name)
            }
            return true

        } else if (cmd.name == "plugins") {
            var list = ""

            val i = server.pluginManager.activePlugins().iterator()
            while (i.hasNext()) {
                val plugin = i.next()
                list += plugin.name + if (i.hasNext()) ", " else ""
            }

            emitter.sendMessage("#00FFD0$i active server plugins : $list")
            return true

        } else if (cmd.name == "mods") {
            var list = ""
            var i = 0
            for (csp in server.content.modsManager().currentlyLoadedMods) {
                i++
                list += csp.modInfo.name + if (i == server.content.modsManager().currentlyLoadedMods.size) "" else ", "
            }
            emitter.sendMessage("#FF0000$i active server mods : $list")
            return true

        }
        return false
    }

}
