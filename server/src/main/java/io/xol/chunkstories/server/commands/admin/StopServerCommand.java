//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

public class StopServerCommand extends ServerCommandBasic {

	public StopServerCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("stop").setHandler(this);
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
