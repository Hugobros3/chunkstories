package io.xol.chunkstories.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.WorldInfo.WorldSize;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.math.Math2;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ClientWorldController implements ClientSideController
{
	Client client;
	WorldClientCommon world;
	
	EntityControllable controlledEntity;
	
	Set<Integer> dontWasteTimeDude = new HashSet<Integer>();
	Set<ChunkHolder> usedChunks = new HashSet<ChunkHolder>();
	Set<RegionSummary> usedRegionSummaries = new HashSet<RegionSummary>();
	
	ClientWorldController(Client client, WorldClientCommon world)
	{
		this.client = client;
		this.world = world;
	}

	@Override
	public InputsManager getInputsManager()
	{
		return Client.inputsManager;
	}

	@Override
	public EntityControllable getControlledEntity()
	{
		return controlledEntity;
	}

	@Override
	public boolean setControlledEntity(EntityControllable entityControllable)
	{
		controlledEntity = entityControllable;
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
		
		for (int chunkX = (cameraChunkX - chunksViewDistance - 1); chunkX < cameraChunkX + chunksViewDistance + 1; chunkX++)
		{
			for (int chunkZ = (cameraChunkZ - chunksViewDistance - 1); chunkZ < cameraChunkZ + chunksViewDistance + 1; chunkZ++)
				for (int chunkY = cameraChunkY - 3; chunkY < cameraChunkY + 3; chunkY++)
				{
					WorldInfo worldInfo = world.getWorldInfo();
					WorldSize size = worldInfo.getSize();
					
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
					
					//System.out.println(holder);
					if(usedChunks.add(holder))
					{
						dontWasteTimeDude.add(summed);
						//System.out.println(b);
						//System.out.println("Registerin'" +holder + " "+ usedChunks.size());
					}
					//Chunk chunk = world.getChunkChunkCoordinates(t, b, g);
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
				WorldSize size = worldInfo.getSize();
				
				int filteredChunkX = holder.getChunkCoordinateX() & (size.maskForChunksCoordinates);
				int filteredChunkY = Math2.clampi(holder.getChunkCoordinateY(), 0, 31);
				int filteredChunkZ = holder.getChunkCoordinateZ() & (size.maskForChunksCoordinates);
				
				int summed = ((filteredChunkX << size.bitlengthOfVerticalChunksCoordinates) | filteredChunkY ) << size.bitlengthOfHorizontalChunksCoordinates | filteredChunkZ;
				
				dontWasteTimeDude.remove(summed);
				
				i.remove();
				holder.unregisterUser(this);
			}
		}
		

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
}
