package io.xol.chunkstories.server;

import java.util.Iterator;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerConsole
{
	Server server;

	public ServerConsole(Server server)
	{
		this.server = server;
	}

	public boolean handleCommand(CommandEmitter emitter, Command cmd, String[] arguments)
	{
		ChunkStoriesLogger.getInstance().info(("[" + emitter.getName() + "] ") + "Entered command : " + cmd.getName());
		try
		{
			//First try to handle the plugins commands
			try
			{
				if (server.getPluginsManager().handleCommand(emitter, cmd, arguments))
					return true;
			}
			catch (Exception e)
			{
				emitter.sendMessage("An exception happened while handling your command : " + e.getLocalizedMessage());
				e.printStackTrace();
			}
			//Then try the world commands

			// No rights needed
			if (cmd.equals("uptime"))
			{
				emitter.sendMessage("#00FFD0The server has been running for " + server.getUptime() + " seconds.");
				return true;
			}
			else if (cmd.equals("info"))
			{
				//System.out.println("<<CUCK SUPRÊME>>");
				emitter.sendMessage("#00FFD0The server's ip is " + server.getHandler().getIP());
				emitter.sendMessage("#00FFD0It's running version " + VersionInfo.version + " of the server software.");
				emitter.sendMessage("#00FFD0" + server.getWorld().getRegionsHolder().toString());
				emitter.sendMessage("#00FFD0" + server.getWorld().ioHandler);
				emitter.sendMessage("#00FFD0" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb used out of " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb allocated");
				return true;
			}
			else if (cmd.equals("help"))
			{
				emitter.sendMessage("#00FFD0Avaible commands :");
				emitter.sendMessage("#00FFD0" + " /plugins");
				emitter.sendMessage("#00FFD0" + " /list");
				emitter.sendMessage("#00FFD0" + " /info");
				emitter.sendMessage("#00FFD0" + " /uptime");
				for (Command command : server.getPluginsManager().commandsHandlers.keySet())
				{
					emitter.sendMessage("#00FFD0 /" + command.getName());
				}
				return true;

			}
			else if (cmd.equals("plugins"))
			{
				String list = "";
				int i = 0;
				for (ChunkStoriesPlugin csp : server.getPluginsManager().activePlugins)
				{
					i++;
					list += csp.getName() + (i == server.getPluginsManager().activePlugins.size() ? "" : ", ");
				}
				emitter.sendMessage("#00FFD0" + i + " active plugins : " + list);
				return true;

			}
			else if (cmd.equals("list"))
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
			else if (cmd.equals("io"))
			{
				emitter.sendMessage("#00FFD0" + server.getWorld().ioHandler);
				server.getWorld().ioHandler.dumpIOTaks();
				return true;
			}
			else if (cmd.equals("entities"))
			{
				Iterator<Entity> entities = server.getWorld().getAllLoadedEntities();
				while (entities.hasNext())
				{
					Entity entity = entities.next();
					emitter.sendMessage("#FFDD00" + entity);
				}
				return true;
			}
			else if (cmd.equals("tim"))
			{
				emitter.sendMessage("#FFDD00" + ((Player) emitter).getLocation().getWorld());

				return true;
			}
			else if (cmd.equals("save"))
			{
				server.getWorld().saveEverything();
				return true;
			}
			else if (cmd.equals("region"))
			{
				Player player = (Player) emitter;

				emitter.sendMessage("#00FFD0" + player.getControlledEntity().getRegion());
				return true;
			}
			else if (cmd.equals("spawnEntity"))
			{
				int id = Integer.parseInt(arguments[0]);
				Entity test = EntitiesList.newEntity(server.getWorld(), (short) id);
				test.setLocation(((Player) emitter).getLocation());
				server.getWorld().addEntity(test);

				emitter.sendMessage("#00FFD0" + "Spawned " + test);
				return true;
			}
			else if (cmd.equals("fly"))
			{
				if (emitter instanceof Player)
				{
					Player client = ((Player) emitter);

					Entity controlledEntity = client.getControlledEntity();
					if (controlledEntity != null && controlledEntity instanceof EntityFlying)
					{
						boolean state = ((EntityFlying) controlledEntity).getFlyingComponent().isFlying();
						state = !state;
						client.sendMessage("flying : " + state);
						((EntityFlying) controlledEntity).getFlyingComponent().setFlying(state);
						return true;
					}
				}
			}
			else if (cmd.equals("creative"))
			{
				if (emitter instanceof Player)
				{
					Player client = ((Player) emitter);

					Entity controlledEntity = client.getControlledEntity();
					if (controlledEntity != null && controlledEntity instanceof EntityCreative)
					{
						boolean state = ((EntityCreative) controlledEntity).getCreativeModeComponent().isCreativeMode();
						state = !state;
						client.sendMessage("creative : " + state);
						((EntityCreative) controlledEntity).getCreativeModeComponent().setCreativeMode(state);
						return true;
					}
				}
			}
			// Rights check
			if (emitter.hasPermission("server.admin"))
			{
				if (cmd.equals("stop"))
				{
					emitter.sendMessage("Stopping server.");
					server.stop();
					return true;
				}
				else if (cmd.equals("clients"))
				{
					emitter.sendMessage("==Listing clients==");
					Iterator<ServerClient> connectedClientsIterator = server.getHandler().getAllConnectedClients();
					while (connectedClientsIterator.hasNext())
					{
						ServerClient client = connectedClientsIterator.next();
						emitter.sendMessage(client.getIp() + "/" + client.getHost() + " - " + client.name);
					}
					emitter.sendMessage("==done==");
					return true;
				}
				else if (cmd.equals("reloadconfig"))
				{
					server.reloadConfig();
					emitter.sendMessage("Config reloaded.");
					return true;
				}
				else if (arguments.length >= 1 && cmd.equals("kick"))
				{
					ServerClient clientByName = server.getHandler().getAuthentificatedClientByName(arguments[0]);
					String kickReason = "refrain from portraying an imbecile attitude";
					//Recreate the argument
					if (arguments.length >= 2)
					{
						kickReason = "";
						for (int i = 1; i < arguments.length; i++)
							kickReason += arguments[i] + (i < arguments.length - 1 ? " " : "");
					}

					if (clientByName != null)
					{
						clientByName.disconnect("Kicked from server. \n" + kickReason);
						//server.handler.disconnectClient(tokick);
						emitter.sendMessage("Kicked " + clientByName + " for " + kickReason);
					}
					else
					{
						emitter.sendMessage("User '" + clientByName + "' not found.");
					}
					return true;
				}
				else
				{
					emitter.sendMessage("Unrecognized command. Try /help.");
					return true;
				}
			}
		}
		catch (Exception e)
		{
			//Print error stack here
			e.printStackTrace();

			//Tell him
			emitter.sendMessage(e.getMessage());
			return false;
		}

		return false;
	}
}
