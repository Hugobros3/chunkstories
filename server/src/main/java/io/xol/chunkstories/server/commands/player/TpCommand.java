//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.player;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

public class TpCommand extends ServerCommandBasic {

	public TpCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("tp").setHandler(this);
	}
	
	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		
		if(!emitter.hasPermission("server.tp"))
		{
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		
		if(!(emitter instanceof Player))
		{
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}
		
		Player who = (Player)emitter;
		
		Location to = null;

		if (arguments.length == 1) {
			Player otherPlayer = server.getPlayerByName(arguments[0]);
			if (otherPlayer != null)
				to = otherPlayer.getLocation();
			else
				emitter.sendMessage("#FF8966Player not found : " + arguments[0]);
		} else if (arguments.length == 2) {
			who = server.getPlayerByName(arguments[0]);
			if (who == null)
				emitter.sendMessage("#FF8966Player not found : " + arguments[0]);

			Player otherPlayer = server.getPlayerByName(arguments[1]);
			if (otherPlayer != null)
				to = otherPlayer.getLocation();
			else
				emitter.sendMessage("#FF8966Player not found : " + arguments[1]);
		} else if (arguments.length == 3) {
			int x = Integer.parseInt(arguments[0]);
			int y = Integer.parseInt(arguments[1]);
			int z = Integer.parseInt(arguments[2]);

			to = new Location(who.getLocation().getWorld(), x, y, z);
		} else if (arguments.length == 4) {
			who = server.getPlayerByName(arguments[0]);
			if (who == null)
				emitter.sendMessage("#FF8966Player not found : " + arguments[0]);

			int x = Integer.parseInt(arguments[1]);
			int y = Integer.parseInt(arguments[2]);
			int z = Integer.parseInt(arguments[3]);

			to = new Location(who.getLocation().getWorld(), x, y, z);
		}

		if (who != null && to != null) {
			emitter.sendMessage("#FF8966Teleported to : " + to);
			who.setLocation(to);
			return true;
		}
		
		emitter.sendMessage("#FF8966Usage: /tp [who] (<x> <y> <z>)|(to)");
		
		return true;
	}

}
