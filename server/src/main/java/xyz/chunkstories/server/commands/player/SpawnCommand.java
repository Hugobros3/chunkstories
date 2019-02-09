//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;

/** Handles the (re)spawn point of a world */
public class SpawnCommand extends ServerCommandBasic {

	public SpawnCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("spawn").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {

		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;
		if (command.getName().equals("spawn")) {
			if (!emitter.hasPermission("world.spawn")) {
				emitter.sendMessage("You don't have the permission.");
				return true;
			}

			Location loc = player.getWorld().getDefaultSpawnLocation();
			player.setLocation(loc);

			emitter.sendMessage("#00FFD0Teleported to spawn");
			return true;
		} else if (command.getName().equals("setspawn")) {
			if (!emitter.hasPermission("world.spawn.set")) {
				emitter.sendMessage("You don't have the permission.");
				return true;
			}

			Location loc = player.getLocation();
			player.getWorld().setDefaultSpawnLocation(loc);

			emitter.sendMessage("#00FFD0Set default spawn to : " + loc);
			return true;
		}

		return false;
	}

}