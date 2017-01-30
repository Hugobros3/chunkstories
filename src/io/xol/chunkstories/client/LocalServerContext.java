package io.xol.chunkstories.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.entity.PlayerClient;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientLocal;

/**
 * Bridges over Client and Server
 * TODO: Make this share behaviour with an actual Server
 */
public class LocalServerContext implements ClientInterface, ServerInterface
{
	private final Client client;
	
	private final WorldClientLocal world;
	
	public LocalServerContext(Client client)
	{
		this.client = client;
		this.world = (WorldClientLocal) client.getWorld();
	}
	
	@Override
	public Content getContent()
	{
		return client.getContent();
	}

	@Override
	public PluginManager getPluginManager()
	{
		return client.getPluginManager();
	}

	@Override
	public void print(String message)
	{
		client.print(message);
	}

	@Override
	public IterableIterator<Player> getConnectedPlayers()
	{
		Set<Player> players = new HashSet<Player>();
		players.add(Client.getInstance().getPlayer());
			
		return new IterableIterator<Player>()
				{
					Iterator<Player> i = players.iterator();
					@Override
					public boolean hasNext()
					{
						return i.hasNext();
					}
					@Override
					public Player next()
					{
						return i.next();
					}
					@Override
					public Iterator<Player> iterator()
					{
						return this;
					}
			
				};
	}

	@Override
	public Player getPlayerByName(String string)
	{
		return world.getPlayerByName(string);
	}

	@Override
	public Player getPlayerByUUID(long UUID)
	{
		if((long)Client.username.hashCode() == UUID)
			return Client.getInstance().getPlayer();
		
		System.out.println("player by uuid not found"+UUID);
		return null;
	}

	@Override
	public void broadcastMessage(String message)
	{
		printChat(message);
	}

	@Override
	public WorldClientLocal getWorld()
	{
		return (WorldClientLocal) Client.getInstance().getWorld();
	}

	@Override
	public PlayerClient getPlayer()
	{
		return Client.getInstance().getPlayer();
	}

	@Override
	public void printChat(String textToPrint)
	{
		client.printChat(textToPrint);
	}

	@Override
	public void changeWorld(WorldClientCommon world)
	{
		client.changeWorld(world);
	}

	@Override
	public void exitToMainMenu()
	{
		client.exitToMainMenu();
	}

	@Override
	public void exitToMainMenu(String errorMessage)
	{
		client.exitToMainMenu(errorMessage);
	}

	@Override
	public void openInventory(Inventory otherInventory)
	{
		client.openInventory(otherInventory);
	}

	@Override
	public boolean hasFocus()
	{
		return client.hasFocus();
	}

	@Override
	public void reloadAssets()
	{
		client.reloadAssets();
	}

	@Override
	public SoundManager getSoundManager()
	{
		return client.getSoundManager();
	}

	@Override
	public ClientInputsManager getInputsManager()
	{
		return client.getInputsManager();
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return client.getParticlesManager();
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return client.getDecalsManager();
	}

}
