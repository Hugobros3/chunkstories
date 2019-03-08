//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin;

import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;

public class ModerationCommands extends ServerCommandBasic {

	public ModerationCommands(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("kick", this);
		server.getPluginManager().registerCommand("ban", this);
		server.getPluginManager().registerCommand("unban", this);
		server.getPluginManager().registerCommand("banip", this);
		server.getPluginManager().registerCommand("unbanip", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("kick") && emitter.hasPermission("server.admin.kick")) {
			if (arguments.length >= 1) {
				Player clientByName = server.getPlayerByName(arguments[0]);
				String kickReason = "Please refrain from further portrayal of such imbecile attitudes";
				// Recreate the argument
				if (arguments.length >= 2) {
					kickReason = "";
					for (int i = 1; i < arguments.length; i++)
						kickReason += arguments[i] + (i < arguments.length - 1 ? " " : "");
				}

				if (clientByName != null) {
					clientByName.disconnect("Kicked from server. \n" + kickReason);
					// server.handler.disconnectClient(tokick);
					emitter.sendMessage("Kicked " + clientByName + " for " + kickReason);
				} else {
					emitter.sendMessage("User '" + clientByName + "' not found.");
				}
				return true;
			} else {
				emitter.sendMessage("Syntax: /kick <playerName> [reason]");
				return true;
			}
		}
		// TODO ban/unban commands
		return false;
	}

}
