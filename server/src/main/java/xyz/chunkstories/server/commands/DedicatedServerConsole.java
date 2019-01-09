//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands;

import xyz.chunkstories.api.plugin.commands.CommandEmitter;
import xyz.chunkstories.api.plugin.commands.ServerConsole;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.api.util.ColorsTools;
import xyz.chunkstories.server.DedicatedServer;

/**
 * Handles basic commands and forwards not-so-basic commands to plugins Can send
 * command itself
 */
public class DedicatedServerConsole implements ServerConsole {
	private DedicatedServer server;

	public DedicatedServerConsole(DedicatedServer server) {
		this.server = server;
	}

	public boolean dispatchCommand(CommandEmitter emitter, String command, String[] arguments) {
		server.logger().info(("[" + emitter.getName() + "] ") + "Entered command : " + command);

		try {
			if (server.getPluginManager().dispatchCommand(emitter, command, arguments))
				return true;
		} catch (Exception e) {
			emitter.sendMessage("An exception happened while handling your command : " + e.getLocalizedMessage());
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public String getName() {
		return "[SERVER CONSOLE]";
	}

	@Override
	public void sendMessage(String msg) {
		System.out.println(ColorsTools.convertToAnsi("#FF00FF" + msg));
	}

	@Override
	public boolean hasPermission(String permissionNode) {
		// Console has ALL permissions
		return true;
	}

	@Override
	public Server getServer() {
		return server;
	}
}
