//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.DedicatedServer;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

/** A long-dead meme */
public class StopTheFuckingTree extends ServerCommandBasic {

	public StopTheFuckingTree(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("stop").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("stop") && emitter.hasPermission("server.stop")) {
			if (server instanceof DedicatedServer) {
				emitter.sendMessage("Stopping server.");
				((DedicatedServer) server).stop();
				return true;
			} else {
				emitter.sendMessage("This isn't a dedicated server.");
				return true;
			}
		}
		return false;
	}

}
