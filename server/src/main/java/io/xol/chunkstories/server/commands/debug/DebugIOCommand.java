//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.debug;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;
import io.xol.chunkstories.world.WorldImplementation;

public class DebugIOCommand extends ServerCommandBasic{

	public DebugIOCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("io").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("io") && emitter.hasPermission("server.debug"))
		{
			emitter.sendMessage("#00FFD0" + ((WorldImplementation)server.getWorld()).ioHandler);
			((WorldImplementation)server.getWorld()).ioHandler.dumpIOTaks();
			return true;
		}
		return false;
	}

}
