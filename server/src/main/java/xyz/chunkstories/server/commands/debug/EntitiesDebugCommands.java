//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug;

import java.util.Iterator;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.plugin.commands.Command;
import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.server.commands.ServerCommandBasic;

public class EntitiesDebugCommands extends ServerCommandBasic {

	public EntitiesDebugCommands(Server serverConsole) {
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
