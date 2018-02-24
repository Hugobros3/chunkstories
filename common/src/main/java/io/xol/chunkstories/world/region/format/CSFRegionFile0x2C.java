//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.region.format;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.entity.EntitySerializer;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.region.RegionImplementation;

/** This version adds support for distinguishing between generated empty and ungenerated chunks */
public class CSFRegionFile0x2C extends CSFRegionFile
{
	public CSFRegionFile0x2C(RegionImplementation holder, File file)
	{
		super(holder, file);
	}

	static final int air_chunk_magic_number = 0xFFFFFFFF;
	
	public void load(DataInputStream in) throws IOException
	{
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
							
							owner.getChunkHolder(a, b, c).setCompressedData(new CompressedData(buffer, null, null));
						}
						else if(compressedDataSize == air_chunk_magic_number) {
							owner.getChunkHolder(a, b, c).setCompressedData(new CompressedData(null, null, null));
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
				logger().error("Error while loading "+file);
				logger().error("Exception: {}", e);
				//e.printStackTrace(logger().getPrintWriter());
				//e.printStackTrace();
			}
	
			owner.world.entitiesLock.writeLock().unlock();
			
		}
		finally {
			in.close();
		}
	}

	public void save(DataOutputStream dos) throws IOException
	{
		throw new UnsupportedOperationException("Saving in older formats isn't supported.");
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
