//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.world;

import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;

public class WeatherCommand extends ServerCommandBasic {

	public WeatherCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("weather").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (!emitter.hasPermission("world.weather")) {
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;

		if (arguments.length == 1) {
			float overcastFactor = Float.parseFloat(arguments[0]);
			player.getLocation().getWorld().setWeather(overcastFactor);
		} else
			emitter.sendMessage("#82FFDBSyntax : /weather [0.0 - 1.0]");
		return true;
	}

}