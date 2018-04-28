//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.player;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityCreativeMode;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

/** Handles creativity */
public class CreativeCommand extends ServerCommandBasic {

	public CreativeCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("creative").setHandler(this);
	}
	
	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {

		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;
		
		if(!emitter.hasPermission("self.toggleCreative"))
		{
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		
		Entity entity = player.getControlledEntity();
		if(!entity.components.tryWithBoolean(EntityCreativeMode.class, fm -> {
			boolean state = fm.get();
			state = !state;
			player.sendMessage("Creative mode set to: " + state);
			fm.set(state);
			
			return true;
		}))
			emitter.sendMessage("This action doesn't apply to your current entity.");
		
		return true;
	}

}
