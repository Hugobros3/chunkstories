//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.server.commands.ServerCommandBasic;
import io.xol.chunkstories.world.WorldImplementation;

public class SaveCommand extends ServerCommandBasic {

	public SaveCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("save").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("save") && emitter.hasPermission("server.admin.forcesave")) {
			emitter.sendMessage("#00FFD0Saving the world...");
			((WorldImplementation)server.getWorld()).saveEverything();
			return true;
		}
		return false;
	}

}
