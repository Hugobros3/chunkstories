//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug;

import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;
import xyz.chunkstories.task.WorkerThreadPool;

public class DebugTasksCommand extends ServerCommandBasic {

	public DebugTasksCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("tasks").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("tasks") && emitter.hasPermission("server.debug")) {
			emitter.sendMessage("#00FFD0Tasks in the pipeline: " + server.getTasks().submittedTasks());
			((WorkerThreadPool) server.getTasks()).dumpTasks();
			return true;
		}
		return false;
	}

}
