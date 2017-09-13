package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SaveCommandHandler extends ServerCommandBasic {

	public SaveCommandHandler(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("save").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.equals("save") && emitter.hasPermission("server.admin.forcesave"))
		{
			emitter.sendMessage("#00FFD0Saving the world...");
			server.getWorld().saveEverything();
			return true;
		}
		return false;
	}

}
