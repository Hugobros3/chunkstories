package io.xol.chunkstories.world;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.net.packets.PacketTime;
import io.xol.chunkstories.api.net.packets.PacketVoxelUpdate;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.WorldNetworked;
import io.xol.chunkstories.net.PacketsProcessorActual;
import io.xol.chunkstories.net.PacketsProcessorCommon.PendingSynchPacket;
import io.xol.chunkstories.net.packets.PacketSendWorldInfo;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.net.UserConnection;
import io.xol.chunkstories.server.propagation.VirtualServerDecalsManager;
import io.xol.chunkstories.server.propagation.VirtualServerParticlesManager;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.io.IOTasksMultiplayerServer;
import io.xol.engine.sound.sources.VirtualSoundManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldServer extends WorldImplementation implements WorldMaster, WorldNetworked
{
	private Server server;
	
	private VirtualSoundManager virtualServerSoundManager;
	private VirtualServerParticlesManager virtualServerParticlesManager;
	private VirtualServerDecalsManager virtualServerDecalsManager;

	public WorldServer(Server server, WorldInfoFile worldInfo)
	{
		super(server, worldInfo); //new WorldInfoImplementation(new File(worldDir + "/info.world"), new File(worldDir).getName()));

		this.server = server;
		this.virtualServerSoundManager = new VirtualSoundManager(this);
		this.virtualServerParticlesManager = new VirtualServerParticlesManager(this, server);
		this.virtualServerDecalsManager = new VirtualServerDecalsManager(this, server);

		ioHandler = new IOTasksMultiplayerServer(this);
		ioHandler.start();
	}
	
	public Server getServer()
	{
		return server;
	}

	@Override
	public void tick()
	{
		//Update client tracking
		Iterator<Player> pi = this.getPlayers();
		while (pi.hasNext())
		{
			Player player = pi.next();

			//System.out.println("client: "+client);
			if (player.hasSpawned())
			{
				//Update whatever he sees
				player.updateTrackedEntities();
			}
			
			//Update time & weather
			PacketTime packetTime = new PacketTime();
			packetTime.time = this.getTime();
			packetTime.overcastFactor = this.getWeather();
			
			player.pushPacket(packetTime);
		}
		
		processIncommingPackets();
		//TODO this should work per-world
		this.getServer().getHandler().flushAll();
		
		super.tick();
		
		virtualServerSoundManager.update();
	}

	public void handleWorldMessage(UserConnection sender, String message)
	{
		if (message.equals("info"))
		{
			//Sends the construction info for the world, and then the player entity
			//worldInfo.sendInfo(sender);

			PacketSendWorldInfo packet = new PacketSendWorldInfo(worldInfo);
			sender.pushPacket(packet);
			
			
			//TODO only spawn the player when he asks to
			spawnPlayer(sender.getProfile());
		}
		else if (message.equals("respawn"))
		{
			Player player = sender.getProfile();
			if(player == null)
			{
				sender.sendChat("Fuck off ?");
				return;
			}
			else
			{
				//Only allow to respawn if the current entity is null or dead
				if(player.getControlledEntity() == null || (player.getControlledEntity() instanceof EntityLiving && ((EntityLiving)player.getControlledEntity()).isDead()))
				{
					spawnPlayer(sender.getProfile());
					sender.sendChat("Respawning ...");
				}
				else
					sender.sendChat("You're not dead, or you are controlling a non-living entity.");
			}
		}
		if (message.startsWith("getChunkCompressed"))
		{
			//System.out.println(message);
			String[] split = message.split(":");
			int x = Integer.parseInt(split[1]);
			int y = Integer.parseInt(split[2]);
			int z = Integer.parseInt(split[3]);
			((IOTasksMultiplayerServer) ioHandler).requestCompressedChunkSend(x, y, z, sender);
		}
		if (message.startsWith("getChunkSummary") || message.startsWith("getRegionSummary"))
		{
			String[] split = message.split(":");
			int x = Integer.parseInt(split[1]);
			int z = Integer.parseInt(split[2]);
			((IOTasksMultiplayerServer) ioHandler).requestRegionSummary(x, z, sender);
		}
	}
	
	//TODO move into implem
	@Override
	protected int actuallySetsDataAt(int x, int y, int z, int newData, Entity entity)
	{
		newData = super.actuallySetsDataAt(x, y, z, newData, entity);
		if (newData != -1)
		{
			int blocksViewDistance = 256;
			int sizeInBlocks = getWorldInfo().getSize().sizeInChunks * 32;
			PacketVoxelUpdate packet = new PacketVoxelUpdate();
			packet.x = x;
			packet.y = y;
			packet.z = z;
			packet.data = newData;
			Iterator<Player> pi = server.getConnectedPlayers();
			while (pi.hasNext())
			{
				Player player = pi.next();

				Entity clientEntity = player.getControlledEntity();
				if (clientEntity == null)
					continue;
				Location loc = clientEntity.getLocation();
				int plocx = (int)(double) loc.x();
				int plocy = (int)(double) loc.y();
				int plocz = (int)(double) loc.z();
				//TODO use proper configurable values for this
				if (!((LoopingMathHelper.moduloDistance(x, plocx, sizeInBlocks) > blocksViewDistance + 2) || (LoopingMathHelper.moduloDistance(z, plocz, sizeInBlocks) > blocksViewDistance + 2) || (y - plocy) > 4 * 32))
				{
					player.pushPacket(packet);
				}

			}
		}
		return newData;
	}

	@Override
	public void processIncommingPackets()
	{
		entitiesLock.writeLock().lock();
		
		/*Iterator<ServerToClientConnection> clientsIterator = server.getHandler().getAuthentificatedClients();
		while (clientsIterator.hasNext())
		{
			ServerToClientConnection client = clientsIterator.next();

			//Get buffered packets from this player
			PendingSynchPacket packet = client.getPacketsProcessor().getPendingSynchPacket();
			while (packet != null)
			{
				packet.process(client, client.getPacketsProcessor());
				packet = client.getPacketsProcessor().getPendingSynchPacket();
			}

		}*/
		
		Iterator<Player> clientsIterator = this.getPlayers();
		while (clientsIterator.hasNext())
		{
			UserConnection playerConnection = ((ServerPlayer)clientsIterator.next()).getPlayerConnection();

			//Get buffered packets from this player
			PendingSynchPacket packet = ((PacketsProcessorActual)playerConnection.getPacketsProcessor()).getPendingSynchPacket();
			while (packet != null)
			{
				packet.process(playerConnection, playerConnection.getPacketsProcessor());
				packet = ((PacketsProcessorActual)playerConnection.getPacketsProcessor()).getPendingSynchPacket();
			}
		}
		
		entitiesLock.writeLock().unlock();
	}

	@Override
	public VirtualSoundManager getSoundManager()
	{
		return virtualServerSoundManager;
	}

	@Override
	public VirtualServerParticlesManager getParticlesManager()
	{
		return virtualServerParticlesManager;
	}

	@Override
	public VirtualServerDecalsManager getDecalsManager()
	{
		return virtualServerDecalsManager;
	}

	public IterableIterator<Player> getPlayers()
	{
		return new IterableIterator<Player>()
		{
			Iterator<Player> players = server.getConnectedPlayers();
			Player next = null;
			
			@Override
			public boolean hasNext()
			{
				while(next == null && players.hasNext()) {
					next = players.next();
					if(next.getWorld() != null && next.getWorld().equals(WorldServer.this)) //Require the player to be spawned within this world.
						break;
					else
						next = null;
				}
				return next != null;
			}

			@Override
			public Player next()
			{
				Player player = next;
				next = null;
				//System.out.println("Giving up player" +player+", the jew");
				return player;
			}

		};
		//return server.getConnectedPlayers();
	}

	@Override
	public Player getPlayerByName(String playerName)
	{
		//Does the server have this player ?
		Player player = server.getPlayerByName(playerName);
		if(player == null)
			return null;
		
		//We don't want players from other worlds
		if(!player.getWorld().equals(this))
			return null;
		
		return player;
	}
}
