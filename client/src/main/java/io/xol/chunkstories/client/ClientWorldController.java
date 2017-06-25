package io.xol.chunkstories.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.math.LoopingMathHelper;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.PlayerClient;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.world.WorldClientCommon;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ClientWorldController implements PlayerClient
{
	Client client;
	WorldClientCommon world;
	
	EntityControllable controlledEntity;
	
	Set<Integer> dontWasteTimeDude = new HashSet<Integer>();
	Set<ChunkHolder> usedChunks = new HashSet<ChunkHolder>();
	Set<RegionSummary> usedRegionSummaries = new HashSet<RegionSummary>();
	
	//int sanity = 0, sanity2 = 0;
	
	ClientWorldController(Client client, WorldClientCommon world)
	{
		this.client = client;
		this.world = world;
	}

	@Override
	public ClientInputsManager getInputsManager()
	{
		return Client.getInstance().getInputsManager();
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
			
			//If a world master, directly set the entity's controller
			if(world instanceof WorldMaster)
				controllableEntity.getControllerComponent().setController(this);
		
			//In remote networked worlds, we need to subscribe the server to our player actions to the controlled entity so he gets updates
			if(entity.getWorld() instanceof WorldClientNetworkedRemote)
			{
				//When changing controlled entity, first unsubscribe the remote server from the one we no longer own
				if(controlledEntity != null && controllableEntity != controlledEntity)
					((WorldClientNetworkedRemote) controlledEntity.getWorld()).getRemoteServer().unsubscribe(controlledEntity);
				
				//Let know the server of new changes
				((WorldClientNetworkedRemote) controllableEntity.getWorld()).getRemoteServer().subscribe(controllableEntity);
			}
			
			controlledEntity = controllableEntity;
		}
		else if (entity == null && getControlledEntity() != null)
		{
			//Directly unset it
			if(world instanceof WorldMaster)
				getControlledEntity().getControllerComponent().setController(null);

			//When loosing control over an entity, stop sending the server info about it
			if(controlledEntity != null)
				if(controlledEntity.getWorld() instanceof WorldClientNetworkedRemote)
					((WorldClientNetworkedRemote) controlledEntity.getWorld()).getRemoteServer().unsubscribe(controlledEntity);
			
			controlledEntity = null;
		}
		
		return true;
	}
	
	@Override
	public void updateUsedWorldBits()
	{
		if(controlledEntity == null)
			return;
			
		//Subscribe to nearby wanted chunks
		int cameraChunkX = Math2.floor((controlledEntity.getLocation().getX()) / 32);
		int cameraChunkY = Math2.floor((controlledEntity.getLocation().getY()) / 32);
		int cameraChunkZ = Math2.floor((controlledEntity.getLocation().getZ()) / 32);
		int chunksViewDistance = (int) (RenderingConfig.viewDistance / 32);
		
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
	public SoundManager getSoundManager()
	{
		return Client.getInstance().getSoundManager();
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return world.getParticlesManager();
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return world.getDecalsManager();
	}

	@Override
	public long getUUID()
	{
		return Client.username.hashCode();
	}

	@Override
	public Iterator<Entity> getSubscribedToList()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean subscribe(Entity entity)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unsubscribe(Entity entity)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unsubscribeAll()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushPacket(Packet packet)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSubscribedTo(Entity entity)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasFocus()
	{
		return client.hasFocus();
	}

	@Override
	public String getName()
	{
		return Client.username;
	}

	@Override
	public String getDisplayName()
	{
		return getName();
	}

	@Override
	public void sendMessage(String msg)
	{
		client.printChat(msg);
	}

	@Override
	public Location getLocation()
	{
		Entity controlledEntity = this.controlledEntity;
		if(controlledEntity != null)
			return controlledEntity.getLocation();
		return null;
	}

	@Override
	public void setLocation(Location l)
	{
		Entity controlledEntity = this.controlledEntity;
		if(controlledEntity != null)
			controlledEntity.setLocation(l);
	}

	@Override
	public boolean isConnected()
	{
		return true;
	}

	@Override
	public boolean hasSpawned()
	{
		return controlledEntity != null;
	}

	@Override
	public void updateTrackedEntities()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public GameContext getContext()
	{
		return this.client;
	}

	@Override
	public World getWorld()
	{
		Entity controlledEntity = this.controlledEntity;
		if(controlledEntity != null)
			return controlledEntity.getWorld();
		return null;
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		return true;
	}

	@Override
	public void flush()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect()
	{
		
	}

	@Override
	public void disconnect(String disconnectionReason)
	{
		
	}
	
	@Override
	public GameWindow getWindow()
	{
		return this.client.getGameWindow();
	}

	@Override
	public void openInventory(Inventory inventory)
	{
		Entity entity = this.getControlledEntity();
		if (inventory.isAccessibleTo(entity))
		{
			//Directly open it without further concern
			//Client.getInstance().openInventories(inventory);
			
			if(entity != null && entity instanceof EntityWithInventory)
				Client.getInstance().openInventories(((EntityWithInventory) entity).getInventory(), inventory);
			else
				Client.getInstance().openInventories(inventory);
		}
		//else
		//	this.sendMessage("Notice: You don't have access to this inventory.");
	}

	@Override
	public ClientInterface getClient() {
		return this.client;
	}
}
