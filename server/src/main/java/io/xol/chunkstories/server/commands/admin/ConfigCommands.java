package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ConfigCommands extends ServerCommandBasic {

	public ConfigCommands(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("reloadConfig").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("reloadconfig") && emitter.hasPermission("server.reloadConfig"))
		{
			server.reloadConfig();
			emitter.sendMessage("Config reloaded.");
			return true;
		}
		return false;
	}

}
