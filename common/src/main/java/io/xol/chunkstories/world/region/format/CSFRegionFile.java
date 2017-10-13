package io.xol.chunkstories.world.region.format;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import io.xol.chunkstories.api.serialization.OfflineSerializedData;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Common region file storage formats stuff */
public abstract class CSFRegionFile implements OfflineSerializedData {

	public final RegionImplementation owner;
	public final File file;
	
	public CSFRegionFile(RegionImplementation holder, File file)
	{
		this.owner = holder;
		this.file = file;

		//this.file = new File(holder.world.getFolderPath() + "/regions/" + holder.regionX + "." + holder.regionY + "." + holder.regionZ + ".csf");
	}
	
	public AtomicInteger savingOperations = new AtomicInteger();

	public boolean exists() {
		return file.exists();
	}
	
	public abstract void load(DataInputStream in) throws IOException;
	public abstract void save(DataOutputStream out) throws IOException;

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

	public static CSFRegionFile determineVersionAndCreate(RegionImplementation holder) {
		File file = new File(holder.world.getFolderPath() + "/regions/" + holder.regionX + "." + holder.regionY + "." + holder.regionZ + ".csf");
		
		if(file.exists()) {
			
			try {
				FileInputStream fist = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fist);
				
				//Read jsut the first 12 bytes of data
				
				long magicNumber = in.readLong();
				
				int versionNumber = in.readInt();
				//int writeTimestamp = in.readInt();
				
				in.close();
				
				// chnkstrs in ascii
				if(magicNumber == 6003953969960732739L) {
					if(versionNumber == 0x2D)
						return new CSFRegionFile0x2D(holder, file);
					else
						throw new RuntimeException("Unhandled file format revision: " + versionNumber);
				}
				
			}
			catch(IOException e) {
				
			}
		}
		else
			return new CSFRegionFile0x2D(holder, file);
		
		return new CSFRegionFile0x2C(holder, file);
	}

}