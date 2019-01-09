//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin;

import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;
import xyz.chunkstories.world.WorldImplementation;

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
