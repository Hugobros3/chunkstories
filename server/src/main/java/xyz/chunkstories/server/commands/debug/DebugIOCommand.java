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
import xyz.chunkstories.world.WorldImplementation;

public class DebugIOCommand extends ServerCommandBasic {

	public DebugIOCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("io").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("io") && emitter.hasPermission("server.debug")) {
			emitter.sendMessage("#00FFD0" + ((WorldImplementation) server.getWorld()).getIoHandler());
			((WorldImplementation) server.getWorld()).getIoHandler().dumpIOTaks();
			return true;
		}
		return false;
	}

}
