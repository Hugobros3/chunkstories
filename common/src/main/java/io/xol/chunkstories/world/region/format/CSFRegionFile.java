package io.xol.chunkstories.world.region.format;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import io.xol.chunkstories.api.serialization.OfflineSerializedData;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Common region file storage formats stuff */
public abstract class CSFRegionFile implements OfflineSerializedData {

	protected RegionImplementation owner;
	protected File file;
	
	public CSFRegionFile(RegionImplementation holder)
	{
		this.owner = holder;

		this.file = new File(holder.world.getFolderPath() + "/regions/" + holder.regionX + "." + holder.regionY + "." + holder.regionZ + ".csf");
	}
	
	public AtomicInteger savingOperations = new AtomicInteger();

	public boolean exists() {
		return file.exists();
	}

	public abstract void load() throws IOException;
	public abstract void save() throws IOException;

	public void finishSavingOperations() {
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