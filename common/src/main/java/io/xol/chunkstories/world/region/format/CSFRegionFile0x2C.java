package io.xol.chunkstories.world.region.format;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityUnsaveable;
import io.xol.chunkstories.entity.EntitySerializer;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** This version adds support for distinguishing between generated empty and ungenerated chunks */
public class CSFRegionFile0x2C extends CSFRegionFile
{
	public CSFRegionFile0x2C(RegionImplementation holder)
	{
		super(holder);
	}

	static final int air_chunk_magic_number = 0xFFFFFFFF;
	
	public void load() throws IOException
	{
		FileInputStream fist = new FileInputStream(file);
		DataInputStream in = new DataInputStream(fist);
		
		try {
			// First load the compressed chunk data sizes
			int[] chunksSizes = new int[8 * 8 * 8];
			for (int a = 0; a < 8 * 8 * 8; a++)
			{
				chunksSizes[a] = in.readInt();
			}
	
			//Load in the compressed chunks
			for (int a = 0; a < 8; a++)
				for (int b = 0; b < 8; b++)
					for (int c = 0; c < 8; c++)
					{
						int compressedDataSize = chunksSizes[a * 8 * 8 + b * 8 + c];
						
						//Compressed data was found, load it
						if (compressedDataSize > 0) {
							byte[] buffer = new byte[compressedDataSize];
							in.readFully(buffer, 0, compressedDataSize);
							owner.getChunkHolder(a, b, c).setCompressedData(buffer);
						}
						else if(compressedDataSize == air_chunk_magic_number) {
							owner.getChunkHolder(a, b, c).setCompressedData(ChunkHolderImplementation.AIR_CHUNK_NO_DATA_SAVED);
						}
						else if(compressedDataSize == 0x00000000){
							owner.getChunkHolder(a, b, c).setCompressedData(null);
						}
						else {
							throw new RuntimeException("Unexpected negative length for compressed chunk size: "+compressedDataSize);
						}
					}
	
			//We pretend it's loaded sooner so we can add the entities and they will load their voxel data if needed
			owner.setDiskDataLoaded(true);
	
			//don't tick the world entities until we get this straight
			owner.world.entitiesLock.writeLock().lock();
	
			//Older version case - TODO write a version mechanism that prevents from checking this
			if (in.available() <= 0)
			{
				owner.world.entitiesLock.writeLock().unlock();
				return;
			}
	
			try
			{
				//Read entities until we hit -1
				Entity entity = null;
				do
				{
					entity = EntitySerializer.readEntityFromStream(in, this, owner.world);
					if (entity != null)
						owner.world.addEntity(entity);
				}
				while (entity != null);
	
			}
			catch (Exception e)
			{
				ChunkStoriesLoggerImplementation.getInstance().info("Error while loading "+file);
				e.printStackTrace(ChunkStoriesLoggerImplementation.getInstance().getPrintWriter());
				e.printStackTrace();
			}
	
			owner.world.entitiesLock.writeLock().unlock();
			
		}
		finally {
			in.close();
		}
	}

	public void save() throws IOException
	{
		//Create the necessary directory structure if needed
		file.getParentFile().mkdirs();
		//if (!file.exists())
		//	file.createNewFile();
		
		FileOutputStream oute = new FileOutputStream(file);
		DataOutputStream dos = new DataOutputStream(oute);
		
		try {
			byte[][][][] compressedVersions = new byte[8][8][8][];
			
			//First we write the header
			for (int a = 0; a < 8; a++)
				for (int b = 0; b < 8; b++)
					for (int c = 0; c < 8; c++)
					{
						byte[] chunkCompressedVersion = owner.getChunkHolder(a, b, c).getCompressedData();
						int chunkSize = 0;
						
						if(chunkCompressedVersion == ChunkHolderImplementation.AIR_CHUNK_NO_DATA_SAVED) {
							chunkSize = air_chunk_magic_number;
						}
						else if (chunkCompressedVersion != null)
						{
							//Save the reference to ensure coherence with later part (in case chunk gets re-compressed in the meantime)
							compressedVersions[a][b][c] = chunkCompressedVersion;
							chunkSize = chunkCompressedVersion.length;
						}
						
						dos.writeInt(chunkSize);
					}
			
			for (int a = 0; a < 8; a++)
				for (int b = 0; b < 8; b++)
					for (int c = 0; c < 8; c++)
						if (compressedVersions[a][b][c] != null)
							dos.write(compressedVersions[a][b][c]);
						
			//don't tick the world entities until we get this straight - this is about not duplicating entities
			owner.world.entitiesLock.readLock().lock();
	
			Iterator<Entity> holderEntities = owner.getEntitiesWithinRegion();
			while (holderEntities.hasNext())
			{
				Entity entity = holderEntities.next();
				//Don't save controllable entities
				if (entity.exists() && !(entity instanceof EntityUnsaveable && !((EntityUnsaveable) entity).shouldSaveIntoRegion()))
				{
					EntitySerializer.writeEntityToStream(dos, this, entity);
				}
			}
			//dos.writeLong(-1);
			EntitySerializer.writeEntityToStream(dos, this, null);
			
			owner.world.entitiesLock.readLock().unlock();

		}
		finally {
			dos.close();
		}
	}

	public void finishSavingOperations()
	{
		//Waits out saving operations.
		while (savingOperations.get() > 0)
			//System.out.println(savingOperations.get());
			synchronized (this)
			{
				try
				{
					wait(20L);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
	}

}
