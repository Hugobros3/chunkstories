package io.xol.chunkstories.server.tech;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.server.Command;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.net.packets.PacketEntity;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.server.net.ServerConnectionsHandler;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerConsole
{
	public static void handleCommand(String cmd, CommandEmitter emitter)
	{
		ChunkStoriesLogger.getInstance().info(("[" + emitter.getName() + "] ") + "Entered command : " + cmd);
		try
		{
			//First handle the plugins commands
			try{
				if (Server.getInstance().pluginsManager.dispatchCommand(cmd, emitter))
					return;
			}
			catch(Exception e)
			{
				emitter.sendMessage("An exception happened while handling your command : "+e.getLocalizedMessage());
				e.printStackTrace();
			}
			
			// No rights needed
			if (cmd.equals("uptime"))
			{
				emitter.sendMessage("#00FFD0The server has been running for " + (System.currentTimeMillis() / 1000 - Server.getInstance().initTimestamp) + " seconds.");
				return;
			}
			else if (cmd.equals("info"))
			{
				emitter.sendMessage("#00FFD0The server's ip is " + ServerConnectionsHandler.ip);
				emitter.sendMessage("#00FFD0It's running version " + VersionInfo.version + " of the server software.");
				emitter.sendMessage("#00FFD0"+Server.getInstance().world.getChunksHolder().toString());
				return;
			}
			else if (cmd.equals("help"))
			{
				emitter.sendMessage("#00FFD0Avaible commands :");
				emitter.sendMessage("#00FFD0" + " /plugins");
				emitter.sendMessage("#00FFD0" + " /list");
				emitter.sendMessage("#00FFD0" + " /info");
				emitter.sendMessage("#00FFD0" + " /uptime");
				for (Command command : Server.getInstance().pluginsManager.commandsHandlers.keySet())
				{
					emitter.sendMessage("#00FFD0 /" + command.getName());
				}
				return;

			}
			else if (cmd.equals("plugins"))
			{
				String list = "";
				int i = 0;
				for (ChunkStoriesPlugin csp : Server.getInstance().pluginsManager.activePlugins)
				{
					i++;
					list += csp.getName() + (i == Server.getInstance().handler.clients.size() ? "" : ", ");
				}
				emitter.sendMessage("#00FFD0" + i + " active plugins : " + list);
				return;

			}
			else if (cmd.equals("list"))
			{
				String list = "";
				int i = 0;
				for (ServerClient client : Server.getInstance().handler.clients)
				{
					i++;
					list += client.getProfile().getDisplayName() + (i == Server.getInstance().handler.clients.size() ? "" : ", ");
				}
				emitter.sendMessage("#00FFD0" + i + " players connected : " + list);
				return;
			}
			else if(cmd.equals("fly"))
			{
				if(emitter instanceof Player)
				{
					Player client = ((Player)emitter);

					Entity controlledEntity = client.getControlledEntity();
					if(controlledEntity != null)
					{
						boolean state = controlledEntity.isFlying();
						state = !state;
						client.sendMessage("Flying : "+state);
						controlledEntity.setFlying(state);
						return;
					}
				}
			}
			// Rights check
			if (emitter.hasPermission("server.admin"))
			{
				if (cmd.equals("stop"))
				{
					emitter.sendMessage("Stopping server.");
					Server.getInstance().stop();
					return;
				}
				else if (cmd.equals("clients"))
				{
					emitter.sendMessage("==Listing clients==");
					for (ServerClient client : Server.getInstance().handler.clients)
					{
						emitter.sendMessage(client.getIp() + "/" + client.getHost() + " - " + client.name);
					}
					emitter.sendMessage("==done==");
					return;
				}
				else if (cmd.equals("reloadconfig"))
				{
					Server.getInstance().reloadConfig();
					emitter.sendMessage("Config reloaded.");
					return;
				}
				// net
				else if (cmd.split(" ")[0].equals("kick") && cmd.split(" ").length == 2)
				{
					ServerClient tokick = Server.getInstance().handler.getClientByName(cmd.split(" ")[1]);
					if (tokick != null)
					{
						Server.getInstance().handler.disconnectClient(tokick);
						emitter.sendMessage("Forced disconnect for user " + cmd.split(" ")[1]);
					}
					else
					{
						emitter.sendMessage("User '" + tokick + "' not found.");
					}
					return;
				}
				else if (cmd.split(" ")[0].equals("kickip") && cmd.split(" ").length == 2)
				{
					String tokick = cmd.split(" ")[1];
					Server.getInstance().handler.disconnectClientByIp(tokick);
					emitter.sendMessage("Forced disconnect for ip " + tokick);
					return;
				}
				// help
				else if (cmd.equals("help"))
				{
					emitter.sendMessage("stop - Stops the server.");

					emitter.sendMessage("kickip - Will force disconnect that ip. May kick multiple people on it.");
					emitter.sendMessage("kick - Will force disconnect that user.");
					emitter.sendMessage("ban - Will refuse any connection from this user. Redo to unban.");
					emitter.sendMessage("banip - Will refuse any connection from this IP. Redo to unban.");
					// game
					return;
				}
				else
				{
					emitter.sendMessage("Unrecognized command. Try help.");
				}
			}
		}
		catch (Exception e)
		{
			emitter.sendMessage(e.getMessage());
		}
	}
}
