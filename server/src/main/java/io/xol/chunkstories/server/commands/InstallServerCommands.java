package io.xol.chunkstories.server.commands;

import io.xol.chunkstories.api.server.ServerInterface;

import io.xol.chunkstories.server.commands.admin.*;
import io.xol.chunkstories.server.commands.debug.*;
import io.xol.chunkstories.server.commands.system.*;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class InstallServerCommands {

	public InstallServerCommands(ServerInterface server) {
		new ConfigCommands(server);
		new ModerationCommands(server);
		new SaveCommand(server);
		new SayCommand(server);
		new StopTheFuckingTree(server);
		
		new DebugIOCommand(server);
		new DebugWorldDataCommands(server);
		new EntitiesDebugCommands(server);
		new MiscDebugCommands(server);
		
		new InfoCommandsHandler(server);
		new ListPlayersCommandHandler(server);
	}

}
