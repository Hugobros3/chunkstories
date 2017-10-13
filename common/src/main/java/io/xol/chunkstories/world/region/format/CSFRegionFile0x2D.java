package io.xol.chunkstories.world.region.format;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** This version adds support for distinguishing between generated empty and ungenerated chunks */
public class CSFRegionFile0x2D extends CSFRegionFile
{
	public CSFRegionFile0x2D(RegionImplementation holder, File file)
	{
		super(holder, file);
	}

	static final int air_chunk_magic_number = 0xFFFFFFFF;
	
	public void load(DataInputStream in) throws IOException
	{	
		try {
			
			long magicNumber = in.readLong();
			
			assert magicNumber == 6003953969960732739L;
			
			int versionNumber = in.readInt();
			int writeTimestamp = in.readInt();
			
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
							
							//Load voxels section if it exists
							int voxel_data_size = in.readInt();
							byte[] voxelData = null;
							if(voxel_data_size > 0) {
								voxelData = new byte[voxel_data_size];
								in.readFully(voxelData);
							}

							//Load voxels components section if it exists
							int voxel_components_size = in.readInt();
							byte[] voxelComponentsData = null;
							if(voxel_components_size > 0) {
								voxelComponentsData = new byte[voxel_components_size];
								in.readFully(voxelComponentsData);
							}

							//Load entity section if it exists
							int entities_size = in.readInt();
							byte[] entitiesData = null;
							if(entities_size > 0) {
								entitiesData = new byte[entities_size];
								in.readFully(entitiesData);
							}
							
							System.out.println(compressedDataSize + "vs : " + (voxel_data_size + voxel_components_size + entities_size + 4));
							
							owner.getChunkHolder(a, b, c).setCompressedData(new CompressedData(voxelData, voxelComponentsData, entitiesData));
						}
						//No data exists here
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
			/*owner.world.entitiesLock.writeLock().lock();
	
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
	
			owner.world.entitiesLock.writeLock().unlock();*/
			
			// Load in the voxel components yay
			
		}
		finally {
			in.close();
		}
	}

	public void save(DataOutputStream dos) throws IOException
	{	
		try {
			//Write the 16-byte header
			dos.writeLong(6003953969960732739L);
			dos.writeInt(0x2D);
			dos.writeInt(2017); //TODO proper timestamp
			
			CompressedData[][][] allCompressedData = new CompressedData[8][8][8];

			//we write the index header
			for (int a = 0; a < 8; a++)
				for (int b = 0; b < 8; b++)
					for (int c = 0; c < 8; c++)
					{
						//For each chunk within the region, grab the compressed data version
						CompressedData compressedData = owner.getChunkHolder(a, b, c).getCompressedData();
						
						allCompressedData[a][b][c] = compressedData;
						
						if(compressedData != null)
							dos.writeInt(compressedData.getTotalCompressedSize());
						else // No data found (==> meaning this is an ungenerated chunk)
							dos.writeInt(0);
					}
			
			//Then write the relevant info where it exists
			for (int a = 0; a < 8; a++)
				for (int b = 0; b < 8; b++)
					for (int c = 0; c < 8; c++)
						if (allCompressedData[a][b][c] != null) {
							CompressedData data = allCompressedData[a][b][c];
							
							//Write each section length then data
							if(data.voxelCompressedData != null) {
								dos.writeInt(data.voxelCompressedData.length);
								dos.write(data.voxelCompressedData);
							} else
								dos.writeInt(0);
							
							if(data.voxelComponentsCompressedData != null) {
								dos.writeInt(data.voxelComponentsCompressedData.length);
								dos.write(data.voxelComponentsCompressedData);
							} else
								dos.writeInt(0);
							
							if(data.entitiesCompressedData != null) {
								dos.writeInt(data.entitiesCompressedData.length);
								dos.write(data.entitiesCompressedData);
							} else
								dos.writeInt(0);
						}
						
			//don't tick the world entities until we get this straight - this is about not duplicating entities
			/*owner.world.entitiesLock.readLock().lock();
	
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
			
			owner.world.entitiesLock.readLock().unlock();*/

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
