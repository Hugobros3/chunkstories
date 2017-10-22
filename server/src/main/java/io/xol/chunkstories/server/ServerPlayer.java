package io.xol.chunkstories.server;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponentInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.net.packets.PacketOpenInventory;
import io.xol.chunkstories.server.net.UserConnection;
import io.xol.chunkstories.server.propagation.VirtualServerDecalsManager.ServerPlayerVirtualDecalsManager;
import io.xol.chunkstories.server.propagation.VirtualServerParticlesManager.ServerPlayerVirtualParticlesManager;
import io.xol.chunkstories.world.WorldServer;
import io.xol.engine.misc.ConfigFile;
import io.xol.engine.sound.sources.VirtualSoundManager.ServerPlayerVirtualSoundManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerPlayer implements Player
{
	private ConfigFile playerDataFile;
	private UserConnection playerConnection;

	//Entity controlled
	private EntityControllable controlledEntity;

	//Streaming control
	private Set<Entity> subscribedEntities = ConcurrentHashMap.newKeySet();

	//Mirror of client inputs
	private ServerInputsManager serverInputsManager;

	//Dummy managers to relay synchronisation stuff
	private ServerPlayerVirtualSoundManager virtualSoundManager;
	private ServerPlayerVirtualParticlesManager virtualParticlesManager;
	private ServerPlayerVirtualDecalsManager virtualDecalsManager;

	public ServerPlayer(UserConnection playerConnection)
	{
		this.playerConnection = playerConnection;

		this.playerDataFile = new ConfigFile("./players/" + getPlayerConnection().name.toLowerCase() + ".cfg");

		this.serverInputsManager = new ServerInputsManager(this);

		//TODO this should be reset when the user changes world
		this.virtualSoundManager = playerConnection.getServer().getWorld().getSoundManager().new ServerPlayerVirtualSoundManager(this);
		this.virtualParticlesManager = playerConnection.getServer().getWorld().getParticlesManager().new ServerPlayerVirtualParticlesManager(this);
		this.virtualDecalsManager = playerConnection.getServer().getWorld().getDecalsManager().new ServerPlayerVirtualDecalsManager(this);

		// Sets dates
		this.playerDataFile.setString("lastlogin", "" + System.currentTimeMillis());
		if (this.playerDataFile.getString("firstlogin", "nope").equals("nope"))
			this.playerDataFile.setString("firstlogin", "" + System.currentTimeMillis());

		//Does not create a player entity here, this is taken care of by the player (re)spawn req
	}

	public UserConnection getPlayerConnection()
	{
		return playerConnection;
	}

	public void updateTrackedEntities()
	{
		//Checks ...
		if (controlledEntity == null)
			return;
		//Cache (idk if HotSpot makes it redudant but whatever)
		double ws = controlledEntity.getWorld().getWorldSize();
		Location controlledEntityLocation = controlledEntity.getLocation();
		Iterator<Entity> iter = controlledEntity.getWorld().getAllLoadedEntities();
		Entity e;
		boolean shouldTrack = false;
		//Let's iterate throught all of the world for now
		//TODO don't
		while (iter.hasNext())
		{
			e = iter.next();

			Location loc = e.getLocation();
			//Distance calculations
			double dx = LoopingMathHelper.moduloDistance(controlledEntityLocation.x(), loc.x(), ws);
			double dy = Math.abs(controlledEntityLocation.y() - loc.y());
			double dz = LoopingMathHelper.moduloDistance(controlledEntityLocation.z(), loc.z(), ws);
			shouldTrack = (dx < 256 && dz < 256 && dy < 256);
			boolean contains = subscribedEntities.contains(e) && e.shouldBeTrackedBy(this);
			//Too close and untracked
			if (shouldTrack && !contains)
			{
				this.subscribe(e);
			}
			//Too far but still tracked
			if (!shouldTrack && contains)
			{
				this.unsubscribe(e);

				subscribedEntities.remove(e); // Reminder, we are iterating the world, not trackedEntities
			}
		}

		Iterator<Entity> iter2 = subscribedEntities.iterator();
		while (iter2.hasNext())
		{
			e = iter2.next();

			Location loc = e.getLocation();
			//Distance calculations
			double dx = LoopingMathHelper.moduloDistance(controlledEntityLocation.x(), loc.x(), ws);
			double dy = Math.abs(controlledEntityLocation.y() - loc.y());
			double dz = LoopingMathHelper.moduloDistance(controlledEntityLocation.z(), loc.z(), ws);
			shouldTrack = (dx < 256 && dz < 256 && dy < 256);

			//Reasons other than distance to stop tracking this entity
			if (!e.shouldBeTrackedBy(this) || !shouldTrack)
			{
				this.unsubscribe(e);
			}
			else
			{
				// Just send new positions
				// ...
				// No need to do anything as the component system handles the updates
			}
		}
	}

	public void save()
	{
		long lastTime = playerDataFile.getLong("timeplayed", 0);
		long lastLogin = playerDataFile.getLong("lastlogin", 0);

		if (controlledEntity != null)
		{
			//Useless, kept for admin easyness, scripts, whatnot
			Location controlledEntityLocation = controlledEntity.getLocation();
			
			//Safely assumes as a SERVER the world will be master ;)
			WorldMaster world = (WorldMaster) controlledEntityLocation.getWorld();
			
			playerDataFile.setDouble("posX", controlledEntityLocation.x());
			playerDataFile.setDouble("posY", controlledEntityLocation.y());
			playerDataFile.setDouble("posZ", controlledEntityLocation.z());
			playerDataFile.setString("world", world.getWorldInfo().getInternalName());

			//Serializes the whole player entity !!!
			SerializedEntityFile playerEntityFile = new SerializedEntityFile(world.getFolderPath() + "/players/" + this.getName().toLowerCase() + ".csf");
			playerEntityFile.write(controlledEntity);
		}

		//Telemetry (EVIL)
		playerDataFile.setString("timeplayed", "" + (lastTime + (System.currentTimeMillis() - lastLogin)));
		playerDataFile.save();

		System.out.println("Player profile " + getPlayerConnection().name + " saved.");
	}

	public void removePlayerFromWorld()
	{
		if (controlledEntity != null)
		{
			playerConnection.getServer().getWorld().removeEntity(controlledEntity);
		}
		unsubscribeAll();
	}

	@Override
	public String getName()
	{
		return getPlayerConnection().name;
	}

	@Override
	public EntityControllable getControlledEntity()
	{
		return controlledEntity;
	}

	@Override
	public boolean setControlledEntity(EntityControllable entity)
	{
		if (entity instanceof EntityControllable)
		{
			this.subscribe(entity);

			EntityControllable controllableEntity = (EntityControllable) entity;
			controllableEntity.getControllerComponent().setController(this);
			controlledEntity = controllableEntity;
		}
		else if (entity == null && getControlledEntity() != null)
		{
			getControlledEntity().getControllerComponent().setController(null);
			controlledEntity = null;
		}

		return true;
	}

	@Override
	public Location getLocation()
	{
		if (controlledEntity != null)
			return controlledEntity.getLocation();
		return null;
	}

	@Override
	public void setLocation(Location l)
	{
		if (this.controlledEntity != null)
			this.controlledEntity.setLocation(l);
	}

	/*@Override
	public void setFlying(boolean flying)
	{
		if (this.controlledEntity != null && this instanceof EntityFlying)
			((EntityFlying) this.controlledEntity).getFlyingComponent().set(flying);
	}*/

	@Override
	public boolean isConnected()
	{
		return playerConnection.isAlive();
	}

	@Override
	public void sendMessage(String msg)
	{
		getPlayerConnection().sendChat(msg);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public String getDisplayName()
	{
		String name = getName();
		//Hashed username for colour :)
		return ColorsTools.getUniqueColorPrefix(name) + name + "#FFFFFF";
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		return DedicatedServer.server.getPermissionsManager().hasPermission(this, permissionNode);
	}

	public Location getLastPosition()
	{
		if (this.playerDataFile.isFieldSet("posX"))
		{
			return new Location(playerConnection.getServer().getWorld(), playerDataFile.getDouble("posX"), playerDataFile.getDouble("posY"), playerDataFile.getDouble("posZ"));
		}
		return null;
	}

	public boolean hasSpawned()
	{
		if (controlledEntity != null && controlledEntity.exists())
			return true;
		return false;
	}

	@Override
	public long getUUID()
	{
		//TODO make them proper
		return this.getName().hashCode();
	}

	// Entity tracking

	@Override
	public Iterator<Entity> getSubscribedToList()
	{
		return subscribedEntities.iterator();
	}

	@Override
	public boolean subscribe(Entity entity)
	{
		if (subscribedEntities.add(entity))
		{
			entity.subscribe(this);

			//Only the server should ever push all components to a client
			entity.getComponents().pushAllComponents(this);
			return true;
		}
		return false;
	}

	@Override
	public boolean unsubscribe(Entity entity)
	{
		if (entity.unsubscribe(this))
		{
			subscribedEntities.remove(entity);
			return true;
		}
		return false;
	}

	@Override
	public void unsubscribeAll()
	{
		Iterator<Entity> iterator = getSubscribedToList();
		while (iterator.hasNext())
		{
			Entity entity = iterator.next();
			//If one of the entities is controllable ...
			if (entity instanceof EntityControllable)
			{
				EntityControllable controllableEntity = (EntityControllable) entity;
				Controller entityController = controllableEntity.getControllerComponent().getController();
				//If said entity is controlled by this subscriber/player
				if (entityController == this)
				{
					//Set the controller to null
					controllableEntity.getControllerComponent().setController(null);
				}
			}
			entity.unsubscribe(this);
			iterator.remove();
		}
	}

	@Override
	public boolean isSubscribedTo(Entity entity)
	{
		return subscribedEntities.contains(entity);
	}

	// Managers

	@Override
	public InputsManager getInputsManager()
	{
		return serverInputsManager;
	}

	@Override
	public SoundManager getSoundManager()
	{
		return virtualSoundManager;
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return virtualParticlesManager;
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return virtualDecalsManager;
	}

	Set<Integer> dontWasteTimeDude = new HashSet<Integer>();
	//Set<ChunkHolder> usedChunks = new HashSet<ChunkHolder>();
	//Set<RegionSummary> usedRegionSummaries = new HashSet<RegionSummary>();
	
	Set<ChunkHolder> usedChunks = new HashSet<ChunkHolder>();
	//Set<Region> usedRegions = new HashSet<Region>();
	Set<RegionSummary> usedRegionSummaries = new HashSet<RegionSummary>();

	/*@Override
	public void updateUsedWorldBits()
	{
		if (controlledEntity == null)
			return;
		World world = controlledEntity.getWorld();
		if (world == null)
			return;

		int cameraChunkX = Math2.floor((controlledEntity.getLocation().getX()) / 32);
		int cameraChunkY = Math2.floor((controlledEntity.getLocation().getY()) / 32);
		int cameraChunkZ = Math2.floor((controlledEntity.getLocation().getZ()) / 32);

		//Simulated chunks, properly loaded
		int chunksViewDistance = 4;
		for (int chunkX = (cameraChunkX - chunksViewDistance); chunkX < cameraChunkX + chunksViewDistance; chunkX++)
		{
			for (int chunkZ = (cameraChunkZ - chunksViewDistance); chunkZ < cameraChunkZ + chunksViewDistance; chunkZ++)
				for (int chunkY = cameraChunkY - 3; chunkY < cameraChunkY + 3; chunkY++)
				{
					ChunkHolder holder = world.aquireChunkHolder(this, chunkX, chunkY, chunkZ);
					if (holder == null)
						continue;

					if (usedChunks.add(holder))
					{

					}
				}
		}

		// ( Removes too far ones )
		Iterator<ChunkHolder> chunkHoldersIterator = usedChunks.iterator();
		while (chunkHoldersIterator.hasNext())
		{
			ChunkHolder holder = chunkHoldersIterator.next();
			if ((LoopingMathHelper.moduloDistance(holder.getChunkCoordinateX(), cameraChunkX, world.getSizeInChunks()) > chunksViewDistance + 1)
					|| (LoopingMathHelper.moduloDistance(holder.getChunkCoordinateZ(), cameraChunkZ, world.getSizeInChunks()) > chunksViewDistance + 1) || (Math.abs(holder.getChunkCoordinateY() - cameraChunkY) > 4))
			{
				chunkHoldersIterator.remove();
				holder.unregisterUser(this);
			}
		}

		//Unsimulated regions to send blocks
		int maxChunksViewDistance = 256 / 32;
		int maxRegionsViewDistance = 1;

		int rx = cameraChunkX / 8;
		int ry = cameraChunkY / 8;
		int rz = cameraChunkZ / 8;

		for (int chunkX = (cameraChunkX - maxChunksViewDistance); chunkX < Math.ceil((cameraChunkX + maxChunksViewDistance) / 8.0) * 8.0; chunkX += 8)
		{
			for (int chunkZ = (cameraChunkZ - maxChunksViewDistance); chunkZ < Math.ceil((cameraChunkZ + maxChunksViewDistance) / 8.0) * 8.0; chunkZ += 8)
				for (int chunkY = cameraChunkY - 3; chunkY < cameraChunkY + 3; chunkY++)
				{
					Region region = world.aquireRegionChunkCoordinates(this, chunkX, chunkY, chunkZ);
					if (region == null)
						continue;

					if (usedRegions.add(region))
					{

					}
				}
		}

		// ( Removes too far ones )
		Iterator<Region> regionsIterator = usedRegions.iterator();
		while (regionsIterator.hasNext())
		{
			Region region = regionsIterator.next();

			if ((LoopingMathHelper.moduloDistance(region.getRegionX(), rx, world.getSizeInChunks() / 8) > maxRegionsViewDistance) || (LoopingMathHelper.moduloDistance(region.getRegionZ(), rz, world.getSizeInChunks() / 8) > maxRegionsViewDistance)
					|| (Math.abs(region.getRegionY() - ry) > 1))
			{
				regionsIterator.remove();
				region.unregisterUser(this);
			}
		}

		//Loads / unloads summaries
		int summaryDistance = 32;

		for (int chunkX = (cameraChunkX - summaryDistance); chunkX < cameraChunkX + summaryDistance; chunkX++)
			for (int chunkZ = (cameraChunkZ - summaryDistance); chunkZ < cameraChunkZ + summaryDistance; chunkZ++)
			{
				if (chunkX % 8 == 0 && chunkZ % 8 == 0)
				{
					int regionX = chunkX / 8;
					int regionZ = chunkZ / 8;

					RegionSummary s = world.getRegionsSummariesHolder().aquireRegionSummary(this, regionX, regionZ);
					if (s != null)
						//System.out.println("kek me up inside "+s);
						if (s != null && usedRegionSummaries.add(s))
						{
							//System.out.println("Added "+s + "to summaries used ("+usedRegionSummaries.size()+")");
						}
				}
			}

		// ( Removes too far ones )
		int distInRegions = summaryDistance / 8;
		int s = world.getSizeInChunks() / 8;

		Iterator<RegionSummary> iterator = usedRegionSummaries.iterator();
		while (iterator.hasNext())
		{
			RegionSummary entry = iterator.next();
			int lx = entry.getRegionX();
			int lz = entry.getRegionZ();

			int dx = LoopingMathHelper.moduloDistance(rx, lx, s);
			int dz = LoopingMathHelper.moduloDistance(rz, lz, s);

			if (dx > distInRegions || dz > distInRegions)
			{
				entry.unregisterUser(this);
				iterator.remove();
			}
		}

	}*/
	
	@Override
	public void updateUsedWorldBits()
	{
		if(controlledEntity == null)
			return;
		
		World world = controlledEntity.getWorld();
		if (world == null)
			return;
			
		//Subscribe to nearby wanted chunks
		int cameraChunkX = Math2.floor((controlledEntity.getLocation().x()) / 32);
		int cameraChunkY = Math2.floor((controlledEntity.getLocation().y()) / 32);
		int cameraChunkZ = Math2.floor((controlledEntity.getLocation().z()) / 32);
		//TODO config this
		int chunksViewDistance = (int) (256 / 32);
		
		for (int chunkX = (cameraChunkX - chunksViewDistance - 1); chunkX <= cameraChunkX + chunksViewDistance + 1; chunkX++)
		{
			for (int chunkZ = (cameraChunkZ - chunksViewDistance - 1); chunkZ <= cameraChunkZ + chunksViewDistance + 1; chunkZ++)
				for (int chunkY = cameraChunkY - 3; chunkY <= cameraChunkY + 3; chunkY++)
				{
					WorldInfo worldInfo = world.getWorldInfo();
					WorldInfo.WorldSize size = worldInfo.getSize();
					
					int filteredChunkX = chunkX & (size.maskForChunksCoordinates);
					int filteredChunkY = Math2.clampi(chunkY, 0, 31);
					int filteredChunkZ = chunkZ & (size.maskForChunksCoordinates);
					
					int summed = ((filteredChunkX << size.bitlengthOfVerticalChunksCoordinates) | filteredChunkY ) << size.bitlengthOfHorizontalChunksCoordinates | filteredChunkZ;
					
					/*int recreatedX = (summed >> (size.bitlengthOfHorizontalChunksCoordinates + size.bitlengthOfVerticalChunksCoordinates)) & size.maskForChunksCoordinates;
					int recreatedY = (summed >> (size.bitlengthOfHorizontalChunksCoordinates)) & 31;
					int recreatedZ = (summed) & size.maskForChunksCoordinates;

					System.out.println("Debug"+size+" :/ "+size.maskForChunksCoordinates);
					System.out.println(chunkX+":"+chunkY+":"+chunkZ);
					System.out.println(filteredChunkX+":"+filteredChunkY+":"+filteredChunkZ);
					System.out.println(summed);
					System.out.println(recreatedX+":"+recreatedY+":"+recreatedZ);
					System.out.println(dontWasteTimeDude.add(summed));
					
					assert recreatedX == filteredChunkX;
					assert recreatedY == filteredChunkY;
					assert recreatedZ == filteredChunkZ;*/

					if(dontWasteTimeDude.contains(summed))
						continue;
					
					ChunkHolder holder = world.aquireChunkHolder(this, chunkX, chunkY, chunkZ);
					if(holder == null)
						continue;
					
					//sanity++;
					if(usedChunks.add(holder))
					{
						//sanity2++;
						
						dontWasteTimeDude.add(summed);
					}
				}
		}
		
		//Unsubscribe for far ones
		Iterator<ChunkHolder> i = usedChunks.iterator();
		while(i.hasNext())
		{
			ChunkHolder holder = i.next();
			if (		(LoopingMathHelper.moduloDistance(	holder.getChunkCoordinateX(), cameraChunkX, world.getSizeInChunks()) > chunksViewDistance + 1) 
					|| 	(LoopingMathHelper.moduloDistance(	holder.getChunkCoordinateZ(), cameraChunkZ, world.getSizeInChunks()) > chunksViewDistance + 1)
					|| 	(Math.abs(							holder.getChunkCoordinateY() - cameraChunkY) > 4))
			{
				/*System.out.println("rmv");
				System.out.println((LoopingMathHelper.moduloDistance(	holder.getChunkCoordinateX(), cameraChunkX, world.getSizeInChunks()) > chunksViewDistance + 1) 
					+"\n"+ 	(LoopingMathHelper.moduloDistance(	holder.getChunkCoordinateZ(), cameraChunkZ, world.getSizeInChunks()) > chunksViewDistance + 1)
					+"\n"+ 	(Math.abs(							holder.getChunkCoordinateY() - cameraChunkY) > 4) + " > "+(holder.getChunkCoordinateY() - cameraChunkY) + " -> "  +holder.getChunkCoordinateY());
				*/
	
				WorldInfo worldInfo = world.getWorldInfo();
				WorldInfo.WorldSize size = worldInfo.getSize();
				
				int filteredChunkX = holder.getChunkCoordinateX() & (size.maskForChunksCoordinates);
				int filteredChunkY = Math2.clampi(holder.getChunkCoordinateY(), 0, 31);
				int filteredChunkZ = holder.getChunkCoordinateZ() & (size.maskForChunksCoordinates);
				
				int summed = ((filteredChunkX << size.bitlengthOfVerticalChunksCoordinates) | filteredChunkY ) << size.bitlengthOfHorizontalChunksCoordinates | filteredChunkZ;
				
				dontWasteTimeDude.remove(summed);
				
				//sanity--;
				//sanity2--;
				i.remove();
				holder.unregisterUser(this);
			}
		}
		
		/*System.out.println("Printing debug info");
		Vector3f cameraReference = new Vector3f(cameraChunkX * 32, cameraChunkY * 32, cameraChunkZ * 32);
		for(Chunk c : world.getAllLoadedChunks())
		{
			Vector3f chunkPos = new Vector3f(c.getChunkX() * 32, c.getChunkY() * 32, c.getChunkZ() * 32);
			System.out.println(c.getChunkX()+" "+c.getChunkY()+" "+c.getChunkZ() + "Distance: "+(chunkPos.sub(cameraReference).length()));
		}
		System.out.println("Done.");*/
		
		//System.out.println(sanity+" 2: "+sanity2+" T:"+usedChunks.size());
		//System.out.println("Supposedly used chunks: "+usedChunks.size());
		//System.out.println("Actually loaded chunks: "+world.getRegionsHolder().countChunks());

		int summaryDistance = 32;
		for (int chunkX = (cameraChunkX - summaryDistance); chunkX < cameraChunkX + summaryDistance; chunkX++)
			for (int chunkZ = (cameraChunkZ - summaryDistance); chunkZ < cameraChunkZ + summaryDistance; chunkZ++)
			{
				if(chunkX % 8 == 0 && chunkZ % 8 == 0)
				{
					int regionX = chunkX / 8;
					int regionZ = chunkZ / 8;
					
					RegionSummary s = world.getRegionsSummariesHolder().aquireRegionSummary(this, regionX, regionZ);
					if(s != null)
						//System.out.println("kek me up inside "+s);
					if(s != null && usedRegionSummaries.add(s))
					{
						//System.out.println("Added "+s + "to summaries used ("+usedRegionSummaries.size()+")");
					}
				}
			}
		
		int rx = cameraChunkX / 8;
		int rz = cameraChunkZ / 8;

		int distInRegions = summaryDistance / 8;
		int s = world.getSizeInChunks() / 8;
		//synchronized(summaries)
		{
			Iterator<RegionSummary> iterator = usedRegionSummaries.iterator();
			//Iterator<Entry<Long, RegionSummaryImplementation>> iterator = summaries.entrySet().iterator();
			while (iterator.hasNext())
			{
				RegionSummary entry = iterator.next();
				int lx = entry.getRegionX();
				int lz = entry.getRegionZ();

				int dx = LoopingMathHelper.moduloDistance(rx, lx, s);
				int dz = LoopingMathHelper.moduloDistance(rz, lz, s);
				// System.out.println("Chunk Summary "+lx+":"+lz+" is "+dx+":"+dz+" away from camera max:"+distInRegions+" total:"+summaries.size());
				if (dx > distInRegions || dz > distInRegions)
				{
					//System.out.println("useless "+entry);
					entry.unregisterUser(this);
					iterator.remove();
				}
			}
		}
	}

	@Override
	public WorldServer getWorld()
	{
		if (controlledEntity != null)
			return (WorldServer) getLocation().getWorld();
		return null;
	}

	@Override
	public void pushPacket(Packet packet)
	{
		this.playerConnection.pushPacket(packet);
	}

	@Override
	public void flush()
	{
		this.playerConnection.flush();
	}

	@Override
	public void disconnect()
	{
		this.playerConnection.disconnect();
	}

	@Override
	public void disconnect(String disconnectionReason)
	{
		this.playerConnection.disconnect(disconnectionReason);
	}

	public ServerInterface getServer()
	{
		return playerConnection.getServer();
	}
	
	public ServerInterface getContext() {
		return getServer();
	}

	@Override
	public void openInventory(Inventory inventory)
	{
		Entity entity = this.getControlledEntity();
		if (inventory.isAccessibleTo(entity))
		{
			if (inventory instanceof EntityComponentInventory.EntityInventory)
			{
				//this.sendMessage("Notice: Pushing this inventory to you so you can see the contents");
				EntityComponentInventory.EntityInventory i = (EntityComponentInventory.EntityInventory)inventory;
				i.asComponent().pushComponent(this);
			}

			//this.sendMessage("Sending you the open inventory request.");
			PacketOpenInventory open = new PacketOpenInventory(inventory);
			this.pushPacket(open);
		}
		//else
		//	this.sendMessage("Notice: You don't have access to this inventory.");
	}
}
