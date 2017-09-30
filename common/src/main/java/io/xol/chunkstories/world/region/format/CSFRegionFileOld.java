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
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Older version of the CSF format, without a version tag */
public class CSFRegionFileOld extends CSFRegionFile
{
	public CSFRegionFileOld(RegionImplementation holder) {
		super(holder);
	}

	@Override
	public void load() throws IOException {
		FileInputStream fis = new FileInputStream(file);
		DataInputStream in = new DataInputStream(fis);
		
		// First load the compressed chunk data sizes
		int[] chunksSizes = new int[8 * 8 * 8];
		for (int a = 0; a < 8 * 8 * 8; a++)
		{
			int size = in.read() << 24;
			size += in.read() << 16;
			size += in.read() << 8;
			size += in.read();
			chunksSizes[a] = size;
		}
	
		//Load in the compressed chunks
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					//Get size from before
					int size = chunksSizes[a * 8 * 8 + b * 8 + c];
					if (size > 0)
					{
						byte[] buffer = new byte[size];
						in.readFully(buffer, 0, size);
						owner.getChunkHolder(a, b, c).setCompressedData(buffer);
						// i++;
					}
				}
	
		//We pretend it's loaded sooner so we can add the entities and they will load their chunks data if needed
		owner.setDiskDataLoaded(true);
	
		//don't tick the world entities until we get this straight
		owner.world.entitiesLock.writeLock().lock();
	
		//Older version case
		if (in.available() <= 0)
		{
			//System.out.println("Old version file, no entities to be found anyway");
			in.close();
	
			owner.world.entitiesLock.writeLock().unlock();
			//holder.world.entitiesLock.unlock();
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
		
		in.close();
	}

	@Override
	public void save() throws IOException
	{
		file.getParentFile().mkdirs();
		if (!file.exists())
			file.createNewFile();
		FileOutputStream out = new FileOutputStream(file);
		
		// We obtain a reference to each compressed chunk data and write it's size to form the index
		byte[][][][] compressedVersions = new byte[8][8][8][];
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					int chunkSize = 0;
					byte[] chunkCompressedVersion = owner.getChunkHolder(a, b, c).getCompressedData();
					if (chunkCompressedVersion != null)
					{
						//Save the reference to ensure coherence with later part
						compressedVersions[a][b][c] = chunkCompressedVersion;
						chunkSize = chunkCompressedVersion.length;
					}
					
					out.write((chunkSize >>> 24) & 0xFF);
					out.write((chunkSize >>> 16) & 0xFF);
					out.write((chunkSize >>> 8) & 0xFF);
					out.write((chunkSize >>> 0) & 0xFF);
				}
		// Then write said chunks
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (compressedVersions[a][b][c] != null)
					{
						out.write(compressedVersions[a][b][c]);
					}
				}

		//don't tick the world entities until we get this straight
		owner.world.entitiesLock.readLock().lock();

		DataOutputStream dos = new DataOutputStream(out);

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

		out.close();
	}

}
