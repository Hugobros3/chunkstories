//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.player;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

/** Removes all items from inventory */
public class ClearCommand extends ServerCommandBasic {

	public ClearCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("clear").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (!emitter.hasPermission("self.clearinventory")) {
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		// TODO check the player's entity has an inventory
		Player player = (Player) emitter;
		Entity entity = player.getControlledEntity();
		if (entity != null) {
			entity.traits.with(TraitInventory.class, ei -> {

				player.sendMessage("#FF969BRemoving " + ei.size() + " items from your inventory.");
				ei.clear();
			});
		}

		return true;
	}

}
