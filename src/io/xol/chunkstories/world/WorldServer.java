package io.xol.chunkstories.world;

import java.io.File;
import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.events.PlayerSpawnEvent;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.net.packets.PacketTime;
import io.xol.chunkstories.net.packets.PacketVoxelUpdate;
import io.xol.chunkstories.net.packets.PacketsProcessor.PendingSynchPacket;
import io.xol.chunkstories.server.RemoteServerPlayer;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.net.ServerToClientConnection;
import io.xol.chunkstories.server.propagation.VirtualServerDecalsManager;
import io.xol.chunkstories.server.propagation.VirtualServerParticlesManager;
import io.xol.chunkstories.server.propagation.VirtualServerSoundManager;
import io.xol.chunkstories.world.io.IOTasksMultiplayerServer;
import io.xol.engine.math.LoopingMathHelper;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldServer extends WorldImplementation implements WorldMaster, WorldNetworked
{
	private Server server;
	
	private VirtualServerSoundManager virtualServerSoundManager;
	private VirtualServerParticlesManager virtualServerParticlesManager;
	private VirtualServerDecalsManager virtualServerDecalsManager;

	public WorldServer(Server server, String worldDir)
	{
		super(server, new WorldInfoImplementation(new File(worldDir + "/info.txt"), new File(worldDir).getName()));

		this.server = server;
		this.virtualServerSoundManager = new VirtualServerSoundManager(this, server);
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
		Iterator<Player> pi = server.getConnectedPlayers();
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
		super.tick();
		
		virtualServerSoundManager.update();
	}

	public void handleWorldMessage(ServerToClientConnection sender, String message)
	{
		if (message.equals("info"))
		{
			//Sends the construction info for the world, and then the player entity
			worldInfo.sendInfo(sender);

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
	
	public void spawnPlayer(Player player)
	{
		Entity savedEntity = null;
		
		SerializedEntityFile playerEntityFile = new SerializedEntityFile("./players/" + player.getName().toLowerCase() + ".csf");
		if(playerEntityFile.exists())
			savedEntity = playerEntityFile.read(this);
		
		Location previousLocation = null;
		if(savedEntity != null)
			previousLocation = savedEntity.getLocation();
		
		PlayerSpawnEvent playerSpawnEvent = new PlayerSpawnEvent(player, this, savedEntity, previousLocation);
		server.getPluginManager().fireEvent(playerSpawnEvent);
		
		if(!playerSpawnEvent.isCancelled())
		{
			Entity entity = playerSpawnEvent.getEntity();
			
			Location actualSpawnLocation = playerSpawnEvent.getSpawnLocation();
			if(actualSpawnLocation == null)
				actualSpawnLocation = this.getDefaultSpawnLocation();
			
			if(entity == null || ((entity instanceof EntityLiving) && (((EntityLiving) entity).isDead())))
			{
				entity = new EntityPlayer(this, 0d, 0d, 0d, player.getName());
			}
			else
				entity.setUUID(-1);
			
			entity.setLocation(actualSpawnLocation);
			
			server.getWorld().addEntity(entity);
			if(entity instanceof EntityControllable)
				player.setControlledEntity((EntityControllable) entity);
			else
				System.out.println("Error : entity is not controllable");
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
				int plocx = (int)(double) loc.getX();
				int plocy = (int)(double) loc.getY();
				int plocz = (int)(double) loc.getZ();
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
			ServerToClientConnection playerConnection = ((RemoteServerPlayer)clientsIterator.next()).getPlayerConnection();

			//Get buffered packets from this player
			PendingSynchPacket packet = playerConnection.getPacketsProcessor().getPendingSynchPacket();
			while (packet != null)
			{
				packet.process(playerConnection, playerConnection.getPacketsProcessor());
				packet = playerConnection.getPacketsProcessor().getPendingSynchPacket();
			}
		}
		
		entitiesLock.writeLock().unlock();
	}

	@Override
	public VirtualServerSoundManager getSoundManager()
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
		return server.getConnectedPlayers();
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
