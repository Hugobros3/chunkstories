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

public class SayCommand extends ServerCommandBasic {

	public SayCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("say", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("say") && emitter.hasPermission("server.admin")) {
			String message = "";
			for (String a : arguments) {
				message += a + " ";
			}
			server.broadcastMessage("#FFFF00SERVER: " + message);
		}
		return false;
	}

}
