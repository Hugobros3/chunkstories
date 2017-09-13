package io.xol.chunkstories.server.commands.admin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SayCommandHandler extends ServerCommandBasic {

	public SayCommandHandler(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("say").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.equals("say") && emitter.hasPermission("server.admin"))
		{
			String message = "";
			for(String a : arguments)
			{
				message+=a+" ";
			}
			server.broadcastMessage("#FFFF00SERVER: "+message);
		}
		return false;
	}

}
