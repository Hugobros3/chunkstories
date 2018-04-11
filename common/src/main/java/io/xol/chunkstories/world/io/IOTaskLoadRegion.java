//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.region.format.CSFRegionFile;

public class IOTaskLoadRegion extends IOTask
{
	private static final Logger logger = LoggerFactory.getLogger("world.io");
	RegionImplementation region;

	public IOTaskLoadRegion(RegionImplementation holder)
	{
		this.region = holder;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor)
	{
		//Check no saving operations are occuring
		/*IOTaskSaveRegion saveRegionTask = new IOTaskSaveRegion(region);
		if (tasks != null && tasks.contains(saveRegionTask))
		{
			//System.out.println("A save operation is still running on " + holder + ", waiting for it to complete.");
			return false;
		}*/

		region.handler = CSFRegionFile.determineVersionAndCreate(region);
		
		if (region.file.exists())
		{
			try
			{
				FileInputStream fist = new FileInputStream(region.handler.file);
				DataInputStream in = new DataInputStream(fist);
				
				region.handler.load(in);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				return true;
			}
			catch (IOException e)
			{
				logger.warn("Error loading file"+region.handler.file);
				e.printStackTrace();
				return true;
			}
		}
		//Else if no file exists
		else
		{
			//Generate this crap !
			//region.generateAll();
			//Pre bake phase 1 lightning
		}

		//Marking the holder as loaded allows the game to remove it and unload it, so we set the timer to have a time frame until it naturally unloads.
		region.resetUnloadCooldown();
		region.setDiskDataLoaded(true);

		//world.unloadsUselessData();
		return true;
	}
}