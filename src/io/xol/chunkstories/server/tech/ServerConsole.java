package io.xol.chunkstories.server.tech;

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
			if(Server.getInstance().pluginsManager.dispatchCommand(cmd, emitter))
				return;
			
			// No rights needed
			if (cmd.equals("uptime"))
			{
				emitter.sendMessage("The server has been running for " + (System.currentTimeMillis() / 1000 - Server.getInstance().initS) + " seconds.");
				return;
			}
			else if (cmd.equals("info"))
			{
				emitter.sendMessage("The server's ip is " + ServerConnectionsHandler.ip);
				// emitter.sendMessage("It's running version "+VersionInfo.get()+" of the server software.");
				return;
			}
			else if (cmd.equals("help"))
			{
				emitter.sendMessage("Avaible commands :");
				//Server.getInstance().pluginsManager.l
				return;
				
			}
			else if(cmd.equals("plugins"))
			{
				
			}
			// Rights check
			if(emitter.hasRights("server.admin"))
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
						emitter.sendMessage(client.getIp() + "/" + client.getHost() + ":" + client.id + " - " + client.name);
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
					Server.getInstance().handler.disconnectClient(tokick);
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
		catch(Exception e)
		{
			emitter.sendMessage(e.getMessage());
		}
	}
}
