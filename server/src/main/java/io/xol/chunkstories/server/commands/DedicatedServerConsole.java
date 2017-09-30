package io.xol.chunkstories.server.commands;

import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.ServerConsole;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.server.DedicatedServer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/** Handles basic commands and forwards not-so-basic commands to plugins
 *  Can send command itself */
public class DedicatedServerConsole implements ServerConsole
{
	private DedicatedServer server;

	public DedicatedServerConsole(DedicatedServer server)
	{
		this.server = server;
	}

	public boolean dispatchCommand(CommandEmitter emitter, String command, String[] arguments)
	{
		server.logger().info(("[" + emitter.getName() + "] ") + "Entered command : " + command);
		
		try
		{
			if (server.getPluginManager().dispatchCommand(emitter, command, arguments))
				return true;
		}
		catch (Exception e)
		{
			emitter.sendMessage("An exception happened while handling your command : " + e.getLocalizedMessage());
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public String getName()
	{
		return "[SERVER CONSOLE]";
	}

	@Override
	public void sendMessage(String msg)
	{
		System.out.println(ColorsTools.convertToAnsi("#FF00FF" + msg));
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		// Console has ALL permissions
		return true;
	}

	@Override
	public ServerInterface getServer() {
		return server;
	}
}
