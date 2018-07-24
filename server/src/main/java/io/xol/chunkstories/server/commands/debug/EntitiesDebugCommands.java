//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.debug;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.server.commands.ServerCommandBasic;

public class EntitiesDebugCommands extends ServerCommandBasic {

	public EntitiesDebugCommands(ServerInterface serverConsole) {
		super(serverConsole);
		server.getPluginManager().registerCommand("entities").setHandler(this);
	}

	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {
		if (command.getName().equals("entities") && emitter.hasPermission("server.debug")) {
			Iterator<Entity> entities = server.getWorld().getAllLoadedEntities();
			while (entities.hasNext()) {
				Entity entity = entities.next();
				emitter.sendMessage("#FFDD00" + entity);
			}
			return true;
		}
		return false;
	}

}
