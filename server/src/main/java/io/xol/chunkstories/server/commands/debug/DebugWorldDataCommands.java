package io.xol.chunkstories.server.commands.debug;

import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.server.commands.ServerCommandBasic;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DebugWorldDataCommands extends ServerCommandBasic {

	public DebugWorldDataCommands(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("chunk").setHandler(this);
		server.getPluginManager().registerCommand("region").setHandler(this);
		server.getPluginManager().registerCommand("summary").setHandler(this);
		server.getPluginManager().registerCommand("summaries").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("chunk") && emitter.hasPermission("server.debug"))
		{
			Player player = (Player) emitter;

			emitter.sendMessage("#00FFD0" + player.getControlledEntity().getChunk());
			return true;
		} else if (command.getName().equals("region") && emitter.hasPermission("server.debug"))
		{
			Player player = (Player) emitter;

			Chunk chunk = player.getControlledEntity().getChunk();
			
			if(chunk != null)
				emitter.sendMessage("#00FFD0" + chunk.getRegion());
			else
				emitter.sendMessage("#00FFD0" + "not within a loaded chunk, so no parent region could be found.");
			
			return true;
		} else if (command.getName().equals("summary") && emitter.hasPermission("server.debug")) {
			RegionSummary sum;

			if(arguments.length == 2) {
				int x = Integer.parseInt(arguments[0]);
				int z = Integer.parseInt(arguments[1]);
				sum = server.getWorld().getRegionsSummariesHolder().getRegionSummary(x, z);
			} else {

				Player player = (Player) emitter;
				sum = player.getWorld().getRegionsSummariesHolder().getRegionSummaryLocation(player.getLocation());
			}
			
			emitter.sendMessage("#00FFD0" + sum);
			return true;
		} else if (command.getName().equals("summaries") && emitter.hasPermission("server.debug")) {
			dumpLoadedSummaries((WorldImplementation) server.getWorld(), emitter);
		}
		return false;
	}

	private void dumpLoadedSummaries(WorldImplementation world, CommandEmitter emitter) {
		emitter.sendMessage("#00FFD0" + "Dumping all region summaries...");
		for(RegionSummaryImplementation sum : world.getRegionsSummariesHolder().all()) {
			emitter.sendMessage("#00FFD0" + sum);
		}
	}
}
