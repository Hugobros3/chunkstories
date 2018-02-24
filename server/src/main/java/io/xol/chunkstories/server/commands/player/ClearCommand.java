//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.player;

import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

/** Removes all items from inventory */
public class ClearCommand extends ServerCommandBasic {

	public ClearCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("clear").setHandler(this);
	}
	
	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if(!emitter.hasPermission("self.clearinventory"))
		{
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		//TODO check the player's entity has an inventory
		Player player = (Player) emitter;

		player.sendMessage(
				"#FF969BRemoving " + ((EntityWithInventory) player.getControlledEntity()).getInventory().size()
						+ " items from your inventory.");
		((EntityWithInventory) player.getControlledEntity()).getInventory().clear();

		return true;
	}

}
