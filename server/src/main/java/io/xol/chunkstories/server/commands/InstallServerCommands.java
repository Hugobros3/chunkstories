//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands;

import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.admin.ConfigCommands;
import io.xol.chunkstories.server.commands.admin.ModerationCommands;
import io.xol.chunkstories.server.commands.admin.SaveCommand;
import io.xol.chunkstories.server.commands.admin.SayCommand;
import io.xol.chunkstories.server.commands.admin.StopTheFuckingTree;
import io.xol.chunkstories.server.commands.debug.DebugIOCommand;
import io.xol.chunkstories.server.commands.debug.DebugTasksCommand;
import io.xol.chunkstories.server.commands.debug.DebugWorldDataCommands;
import io.xol.chunkstories.server.commands.debug.EntitiesDebugCommands;
import io.xol.chunkstories.server.commands.debug.MiscDebugCommands;
import io.xol.chunkstories.server.commands.player.ClearCommand;
import io.xol.chunkstories.server.commands.player.CreativeCommand;
import io.xol.chunkstories.server.commands.player.FlyCommand;
import io.xol.chunkstories.server.commands.player.GiveCommand;
import io.xol.chunkstories.server.commands.player.HealthCommand;
import io.xol.chunkstories.server.commands.player.SpawnCommand;
import io.xol.chunkstories.server.commands.player.TpCommand;
import io.xol.chunkstories.server.commands.system.InfoCommands;
import io.xol.chunkstories.server.commands.system.ListPlayersCommand;
import io.xol.chunkstories.server.commands.world.SpawnEntityCommand;
import io.xol.chunkstories.server.commands.world.TimeCommand;
import io.xol.chunkstories.server.commands.world.WeatherCommand;

public class InstallServerCommands {

	public InstallServerCommands(ServerInterface server) {

		// Administration
		new ConfigCommands(server);
		new ModerationCommands(server);
		new SaveCommand(server);
		new SayCommand(server);
		new StopTheFuckingTree(server);

		// Debug
		new DebugIOCommand(server);
		new DebugTasksCommand(server);
		new DebugWorldDataCommands(server);
		new EntitiesDebugCommands(server);
		new MiscDebugCommands(server);

		// Player
		new ClearCommand(server);
		new CreativeCommand(server);
		new FlyCommand(server);
		new GiveCommand(server);
		new HealthCommand(server);
		new SpawnCommand(server);
		new TpCommand(server);

		// System
		new InfoCommands(server);
		new ListPlayersCommand(server);

		// World
		new SpawnEntityCommand(server);
		new TimeCommand(server);
		new WeatherCommand(server);
	}

}
