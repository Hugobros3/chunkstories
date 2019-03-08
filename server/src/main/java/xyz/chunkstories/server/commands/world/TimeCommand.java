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

public class TimeCommand extends ServerCommandBasic {

	public TimeCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("time", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (!emitter.hasPermission("world.time")) {
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;

		if (arguments.length == 1) {
			long newTime = Long.parseLong(arguments[0]);
			player.getLocation().getWorld().setTime(newTime);
			emitter.sendMessage("#82FFDBSet time to  :" + newTime);
		} else
			emitter.sendMessage("#82FFDBSyntax : /time [0-10000]");
		return true;
	}

}
