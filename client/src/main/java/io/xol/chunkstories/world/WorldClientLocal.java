//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.ClientImplementation;
import io.xol.chunkstories.server.LocalServerContext;
import io.xol.chunkstories.world.io.IOTasks;

public class WorldClientLocal extends WorldClientCommon implements WorldMaster {
	protected LocalServerContext localServer;

	public WorldClientLocal(ClientImplementation client, WorldInfoImplementation info) throws WorldLoadingException {
		super(client, info);

		ioHandler = new IOTasks(this);
		ioHandler.start();

		localServer = new LocalServerContext(client, this);
		client.setClientPluginManager(localServer.getPluginManager());
	}

	public LocalServerContext getLocalServer() {
		return localServer;
	}

	@Override
	public ClientImplementation getClient() {
		return ClientImplementation.getInstance();
	}

	@Override
	public SoundManager getSoundManager() {
		// TODO when implementing server/client combo make sure we use something to mix
		// behaviours of WorldServer and this
		return ClientImplementation.getInstance().getSoundManager();
	}

	@Override
	public void tick() {
		// TODO: processIncommingPackets();
		// TODO: flush all
		super.tick();
	}

	@Override
	public IterableIterator<Player> getPlayers() {
		Set<Player> players = new HashSet<Player>();
		if (ClientImplementation.getInstance().getPlayer().hasSpawned())
			players.add(ClientImplementation.getInstance().getPlayer());

		return new IterableIterator<Player>() {
			Iterator<Player> i = players.iterator();

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public Player next() {
				return i.next();
			}

			@Override
			public Iterator<Player> iterator() {
				return this;
			}

		};
		// throw new UnsupportedOperationException("getPlayers");
	}

	@Override
	public Player getPlayerByName(String playerName) {
		if (playerName.equals(ClientImplementation.username))
			return ClientImplementation.getInstance().getPlayer();
		return null;
	}
}
