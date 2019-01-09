//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands;

import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.admin.ConfigCommands;
import xyz.chunkstories.server.commands.admin.ModerationCommands;
import xyz.chunkstories.server.commands.admin.SaveCommand;
import xyz.chunkstories.server.commands.admin.SayCommand;
import xyz.chunkstories.server.commands.admin.StopServerCommand;
import xyz.chunkstories.server.commands.debug.DebugIOCommand;
import xyz.chunkstories.server.commands.debug.DebugTasksCommand;
import xyz.chunkstories.server.commands.debug.DebugWorldDataCommands;
import xyz.chunkstories.server.commands.debug.EntitiesDebugCommands;
import xyz.chunkstories.server.commands.debug.MiscDebugCommands;
import xyz.chunkstories.server.commands.player.ClearCommand;
import xyz.chunkstories.server.commands.player.CreativeCommand;
import xyz.chunkstories.server.commands.player.FlyCommand;
import xyz.chunkstories.server.commands.player.GiveCommand;
import xyz.chunkstories.server.commands.player.HealthCommand;
import xyz.chunkstories.server.commands.player.SpawnCommand;
import xyz.chunkstories.server.commands.player.TpCommand;
import xyz.chunkstories.server.commands.system.InfoCommands;
import xyz.chunkstories.server.commands.system.ListPlayersCommand;
import xyz.chunkstories.server.commands.world.SpawnEntityCommand;
import xyz.chunkstories.server.commands.world.TimeCommand;
import xyz.chunkstories.server.commands.world.WeatherCommand;

public class InstallServerCommands {

	public InstallServerCommands(Server server) {

		// Administration
		new ConfigCommands(server);
		new ModerationCommands(server);
		new SaveCommand(server);
		new SayCommand(server);
		new StopServerCommand(server);

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
