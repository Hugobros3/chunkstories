//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.system;

import java.util.Iterator;

import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

public class ListPlayersCommand extends ServerCommandBasic {

	public ListPlayersCommand(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("list").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("list"))
		{
			String list = "";

			int playersCount = 0;
			Iterator<Player> iterator = server.getConnectedPlayers();
			while (iterator.hasNext())
			{
				playersCount++;

				list += iterator.next().getDisplayName();
				if (iterator.hasNext())
					list += ", ";
			}

			emitter.sendMessage("#00FFD0" + playersCount + " players connected : " + list);
			return true;
		}
		
		return false;
	}

}
