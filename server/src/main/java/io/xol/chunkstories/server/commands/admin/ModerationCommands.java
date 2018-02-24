//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

public class ModerationCommands extends ServerCommandBasic {

	public ModerationCommands(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("kick").setHandler(this);
		server.getPluginManager().registerCommand("ban").setHandler(this);
		server.getPluginManager().registerCommand("unban").setHandler(this);
		server.getPluginManager().registerCommand("banip").setHandler(this);
		server.getPluginManager().registerCommand("unbanip").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("kick") && emitter.hasPermission("server.admin.kick"))
		{
			if(arguments.length >= 1) {
				Player clientByName = server.getPlayerByName(arguments[0]);
				String kickReason = "Please refrain from further portrayal of such imbecile attitudes";
				//Recreate the argument
				if (arguments.length >= 2)
				{
					kickReason = "";
					for (int i = 1; i < arguments.length; i++)
						kickReason += arguments[i] + (i < arguments.length - 1 ? " " : "");
				}

				if (clientByName != null)
				{
					clientByName.disconnect("Kicked from server. \n" + kickReason);
					//server.handler.disconnectClient(tokick);
					emitter.sendMessage("Kicked " + clientByName + " for " + kickReason);
				}
				else
				{
					emitter.sendMessage("User '" + clientByName + "' not found.");
				}
				return true;
			}
			else {
				emitter.sendMessage("Syntax: /kick <playerName> [reason]");
				return true;
			}
		}
		//TODO ban/unban commands
		return false;
	}

}
