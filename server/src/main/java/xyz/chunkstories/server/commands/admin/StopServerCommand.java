//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin;

import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.DedicatedServer;
import xyz.chunkstories.server.commands.ServerCommandBasic;

public class StopServerCommand extends ServerCommandBasic {

	public StopServerCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("stop", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().startsWith("stop") && emitter.hasPermission("server.stop")) {
			if (server instanceof DedicatedServer) {
				emitter.sendMessage("Stopping server.");
				((DedicatedServer) server).stop();
				return true;
			} else {
				//TODO instead of being a smartass just register the command in DedicatedServer.java ?
				emitter.sendMessage("This isn't a dedicated server.");
				return true;
			}
		}
		return false;
	}

}
