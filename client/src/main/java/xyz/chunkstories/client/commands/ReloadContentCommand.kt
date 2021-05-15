//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.commands

import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.plugin.commands.CommandHandler
import xyz.chunkstories.client.glfw.GLFWWindow

class ReloadContentCommand(private val ingameClient: IngameClient) : CommandHandler {

    init {
        ingameClient.pluginManager.registerCommand("reload", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "reload" && emitter.hasPermission("content.reload")) {
            if (arguments.isEmpty()) {
                emitter.sendMessage("#00FFD0" + "Usage for /reload: ")
                emitter.sendMessage("#00FFD0" + "/reload all #FFFFD0| Reloads everything (might break stuff!)")
                //emitter.sendMessage("#00FFD0" + "/reload assets #FFFFD0| Reloads non-code assets without rebuilding the filesystem ( for iteration )")
                emitter.sendMessage("#00FFD0" + "/reload <subsystem> #FFFFD0| Reloads one of the specific hot-reloadable subsystems shown below")
                emitter.sendMessage("#00FFD0" + "/reload plugins #FFFFD0| Reloads only the plugins ( disables them, loads them anew then re-enables them )")

            } else {
                when(val subsystem = arguments[0]) {
                    "all" -> {
                        ingameClient.content.reload()
                        emitter.sendMessage("#00FFD0" + "Reloaded everything.")
                    }
                    "plugins" -> {

                    }
                    "rendergraph" -> {
                        val backend = (ingameClient.engine.gameWindow as GLFWWindow).graphicsEngine.backend
                        backend.reloadRendergraph()

                        emitter.sendMessage("Rendergraph reloaded!")
                    }
                    else -> {
                        emitter.sendMessage("Unknown subsystem: $subsystem")
                    }
                }
            }
            return true
        }
        return false
    }
}
