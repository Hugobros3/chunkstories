//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands;

import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.admin.*;
import io.xol.chunkstories.server.commands.debug.*;
import io.xol.chunkstories.server.commands.player.*;
import io.xol.chunkstories.server.commands.system.*;
import io.xol.chunkstories.server.commands.world.*;

public class InstallServerCommands {

	public InstallServerCommands(ServerInterface server) {
		
		//Administration
		new ConfigCommands(server);
		new ModerationCommands(server);
		new SaveCommand(server);
		new SayCommand(server);
		new StopTheFuckingTree(server);
		
		//Debug
		new DebugIOCommand(server);
		new DebugTasksCommand(server);
		new DebugWorldDataCommands(server);
		new EntitiesDebugCommands(server);
		new MiscDebugCommands(server);
		
		//Player
		new ClearCommand(server);
		new CreativeCommand(server);
		new FlyCommand(server);
		new FoodCommand(server);
		new GiveCommand(server);
		new HealthCommand(server);
		new SpawnCommand(server);
		new TpCommand(server);
		
		//System
		new InfoCommands(server);
		new ListPlayersCommand(server);
		
		//World
		new SpawnEntityCommand(server);
		new TimeCommand(server);
		new WeatherCommand(server);
	}

}
