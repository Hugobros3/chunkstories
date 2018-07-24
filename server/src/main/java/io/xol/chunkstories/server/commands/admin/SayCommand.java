//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

public class SayCommand extends ServerCommandBasic {

	public SayCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("say").setHandler(this);
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
