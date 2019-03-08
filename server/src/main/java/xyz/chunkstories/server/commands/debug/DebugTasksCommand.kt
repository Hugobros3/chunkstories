//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic
import xyz.chunkstories.task.WorkerThreadPool

class DebugTasksCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("tasks", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "tasks" && emitter.hasPermission("server.debug")) {
            emitter.sendMessage("#00FFD0Tasks in the pipeline: " + server.tasks.submittedTasks())
            (server.tasks as WorkerThreadPool).dumpTasks()
            return true
        }
        return false
    }

}
