//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug;

import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.api.world.chunk.Chunk;
import xyz.chunkstories.api.world.heightmap.Heightmap;
import xyz.chunkstories.server.commands.ServerCommandBasic;
import xyz.chunkstories.world.WorldImplementation;
import xyz.chunkstories.world.heightmap.HeightmapImplementation;

public class DebugWorldDataCommands extends ServerCommandBasic {

	public DebugWorldDataCommands(Server serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("chunk", this);
		server.getPluginManager().registerCommand("region", this);
		server.getPluginManager().registerCommand("heightmap", this);
		server.getPluginManager().registerCommand("heightmaps", this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("chunk") && emitter.hasPermission("server.debug")) {
			Player player = (Player) emitter;

			emitter.sendMessage("#00FFD0" + player.getControlledEntity().traitLocation.getChunk());
			return true;
		} else if (command.getName().equals("region") && emitter.hasPermission("server.debug")) {
			Player player = (Player) emitter;

			Chunk chunk = player.getControlledEntity().traitLocation.getChunk();

			if (chunk != null)
				emitter.sendMessage("#00FFD0" + chunk.getRegion());
			else
				emitter.sendMessage("#00FFD0" + "not within a loaded chunk, so no parent region could be found.");

			return true;
		} else if (command.getName().equals("heightmap") && emitter.hasPermission("server.debug")) {
			Heightmap sum;

			if (arguments.length == 2) {
				int x = Integer.parseInt(arguments[0]);
				int z = Integer.parseInt(arguments[1]);
				sum = server.getWorld().getRegionsSummariesHolder().getHeightmap(x, z);
			} else {

				Player player = (Player) emitter;
				sum = player.getWorld().getRegionsSummariesHolder().getHeightmapLocation(player.getLocation());
			}

			emitter.sendMessage("#00FFD0" + sum);
			return true;
		} else if (command.getName().equals("heightmaps") && emitter.hasPermission("server.debug")) {
			dumpLoadedHeightmap((WorldImplementation) server.getWorld(), emitter);
		}
		return false;
	}

	private void dumpLoadedHeightmap(WorldImplementation world, CommandEmitter emitter) {
		emitter.sendMessage("#00FFD0" + "Dumping all region summaries...");
		for (HeightmapImplementation sum : world.getRegionsSummariesHolder().all()) {
			emitter.sendMessage("#00FFD0" + sum);
		}
	}
}
