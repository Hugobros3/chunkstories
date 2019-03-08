//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.serializable.TraitFlyingMode;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;

/** Regulates flying */
public class FlyCommand extends ServerCommandBasic {

	public FlyCommand(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("fly", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {

		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;

		if (!emitter.hasPermission("self.toggleFly")) {
			emitter.sendMessage("You don't have the permission.");
			return true;
		}

		Entity entity = player.getControlledEntity();
		if (!entity.traits.tryWithBoolean(TraitFlyingMode.class, fm -> {
			boolean state = fm.get();
			state = !state;
			player.sendMessage("Flying mode set to: " + state);
			fm.set(state);

			return true;
		}))
			emitter.sendMessage("This action doesn't apply to your current entity.");

		return true;
	}

}
