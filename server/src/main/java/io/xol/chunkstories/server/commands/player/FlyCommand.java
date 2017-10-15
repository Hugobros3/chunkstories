package io.xol.chunkstories.server.commands.player;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Regulates flying */
public class FlyCommand extends ServerCommandBasic {

	public FlyCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("fly").setHandler(this);
	}
	
	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {

		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;
		
		if(!emitter.hasPermission("self.toggleFly"))
		{
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		
		Entity controlledEntity = player.getControlledEntity();
		if (controlledEntity != null && controlledEntity instanceof EntityFlying)
		{
			boolean state = ((EntityFlying) controlledEntity).getFlyingComponent().get();
			state = !state;
			player.sendMessage("Flying mode set to: " + state);
			((EntityFlying) controlledEntity).getFlyingComponent().set(state);
			return true;
		}
		
		emitter.sendMessage("This action doesn't apply to your current entity.");
		
		return true;
	}

}
