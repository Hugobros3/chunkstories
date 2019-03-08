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

public class MiscDebugCommands extends ServerCommandBasic {

	public MiscDebugCommands(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("gc", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("gc") && emitter.hasPermission("server.debug.gc")) {
			emitter.sendMessage("Performing gc...");
			System.gc();
			return true;
		}
		return false;
	}

}
