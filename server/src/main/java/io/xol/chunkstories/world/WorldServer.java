package io.xol.chunkstories.world;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.xol.chunkstories.api.content.OnlineContentTranslator;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketWorld;
import io.xol.chunkstories.api.net.PacketDefinition.PacketGenre;
import io.xol.chunkstories.api.net.packets.PacketTime;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.WorldNetworked;
import io.xol.chunkstories.content.translator.AbstractContentTranslator;
import io.xol.chunkstories.net.LogicalPacketDatagram;
import io.xol.chunkstories.net.PacketDefinitionImpl;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.player.ServerPlayer;
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
	private final DedicatedServer server;
	private final AbstractContentTranslator translator;
	
	private VirtualSoundManager virtualServerSoundManager;
	private VirtualServerParticlesManager virtualServerParticlesManager;
	private VirtualServerDecalsManager virtualServerDecalsManager;

	private Deque<PendingPlayerDatagram> packetsQueue = new ConcurrentLinkedDeque<>();

	public WorldServer(DedicatedServer server, WorldInfoImplementation worldInfo) throws WorldLoadingException
	{
		super(server, worldInfo);
		this.server = server;
		
		this.translator = (AbstractContentTranslator) super.getContentTranslator();
		this.translator.assignPacketIds();
		this.translator.buildArrays();
		
		this.virtualServerSoundManager = new VirtualSoundManager(this);
		this.virtualServerParticlesManager = new VirtualServerParticlesManager(this, server);
		this.virtualServerDecalsManager = new VirtualServerDecalsManager(this, server);

		ioHandler = new IOTasksMultiplayerServer(this);
		ioHandler.start();
	}
	
	public DedicatedServer getServer()
	{
		return server;
	}

	@Override
	public OnlineContentTranslator getContentTranslator() {
		return translator;
	}
	
	@Override
	public void tick()
	{	
		processIncommingPackets();

		super.tick();
		
		//Update client tracking
		Iterator<Player> playersIterator = this.getPlayers();
		while (playersIterator.hasNext())
		{
			Player player = playersIterator.next();

			//System.out.println("client: "+client);
			if (player.hasSpawned())
			{
				//Update whatever he sees
				((ServerPlayer) player).updateTrackedEntities();
			}
			
			//Update time & weather
			PacketTime packetTime = new PacketTime(this);
			packetTime.time = this.getTime();
			packetTime.overcastFactor = this.getWeather();
			player.pushPacket(packetTime);
		}
		
		virtualServerSoundManager.update();
		
		//TODO this should work per-world
		this.getServer().getHandler().flushAll();
	}

	/*public void handleWorldMessage(UserConnection sender, String message)
	{
		if (message.equals("info"))
		{
			//Sends the construction info for the world, and then the player entity
			//worldInfo.sendInfo(sender);
			PacketSendWorldInfo packet = new PacketSendWorldInfo(worldInfo);
			sender.pushPacket(packet);
			
			//TODO only spawn the player when he asks to
			spawnPlayer(sender.getLoggedInPlayer());
		}
		else if (message.equals("respawn"))
		{
			Player player = sender.getLoggedInPlayer();
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
					spawnPlayer(sender.getLoggedInPlayer());
					sender.sendChat("Respawning ...");
				}
				else
					sender.sendChat("You're not dead, or you are controlling a non-living entity.");
			}
		}
	}*/
	
	class PendingPlayerDatagram {
		LogicalPacketDatagram datagram;
		ServerPlayer player;
		public PendingPlayerDatagram(LogicalPacketDatagram datagram, ServerPlayer player) {
			this.datagram = datagram;
			this.player = player;
		}
	}
	
	public void processIncommingPackets()
	{
		entitiesLock.writeLock().lock();
		
		Iterator<PendingPlayerDatagram> iterator = packetsQueue.iterator();
		while (iterator.hasNext())
		{
			PendingPlayerDatagram incomming = iterator.next();
			iterator.remove();
			
			ServerPlayer player = incomming.player;
			LogicalPacketDatagram datagram = incomming.datagram;
			
			try {
				PacketDefinitionImpl definition = (PacketDefinitionImpl) this.getContentTranslator().getPacketForId(datagram.packetTypeId);
				Packet packet = definition.createNew(true, this);
				
				if(definition.getGenre() != PacketGenre.WORLD || !(packet instanceof PacketWorld)) {
					logger().error(definition + " isn't a PacketWorld");
				} else {
					PacketWorld packetWorld = (PacketWorld) packet;
					
					//packetsProcessor.getSender() is equivalent to getRemoteServer() here
					packetWorld.process(player, datagram.getData(), player.getPlayerConnection().getPacketsContext());
				}
			}
			catch(IOException | PacketProcessingException e) {
				logger().warn("Exception while processing datagram: "+e.getMessage());
			}
			
			datagram.dispose();
		}
		
		entitiesLock.writeLock().unlock();
	}

	public void queueDatagram(LogicalPacketDatagram datagram, ServerPlayer player) {
		packetsQueue.addLast(new PendingPlayerDatagram(datagram, player));
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
				return player;
			}

		};
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
