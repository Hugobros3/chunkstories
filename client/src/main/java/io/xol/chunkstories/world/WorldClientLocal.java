package io.xol.chunkstories.world;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.io.IOTasks;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldClientLocal extends WorldClientCommon implements WorldMaster
{
	public WorldClientLocal(Client client, WorldInfoImplementation info)
	{
		super(client, info);
		
		ioHandler = new IOTasks(this);
		ioHandler.start();
	}

	@Override
	public Client getClient()
	{
		return Client.getInstance();
	}

	@Override
	public SoundManager getSoundManager()
	{
		//TODO when implementing server/client combo make sure we use something to mix behaviours of WorldServer and this
		return Client.getInstance().getSoundManager();
	}

	@Override
	public void tick() {
		Iterator<Player> i = getPlayers();
		while(i.hasNext())
		{
			Player p = i.next();
			p.updateUsedWorldBits();
		}
		//TODO: processIncommingPackets();
		//TODO: flush all
		super.tick();
	}
	
	@Override
	public IterableIterator<Player> getPlayers()
	{
		Set<Player> players = new HashSet<Player>();
		if(Client.getInstance().getPlayer().hasSpawned())
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
		//throw new UnsupportedOperationException("getPlayers");
	}

	@Override
	public Player getPlayerByName(String playerName)
	{
		if(playerName.equals(Client.username))
			return Client.getInstance().getPlayer();
		return null;
	}
}
