package io.xol.chunkstories.tools;

import java.io.File;

import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.io.IOTasksImmediate;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldTool extends World
{
	public WorldTool(String worldDir)
	{
		super(new WorldInfo(new File(worldDir+"/info.txt"), new File(worldDir).getName()));
		
		ioHandler = new IOTasksImmediate(this);
		//ioHandler.start();
	}

	public WorldTool(File csWorldDir)
	{
		this(csWorldDir.getAbsolutePath());
	}
	
	@Override
	public void trimRemovableChunks()
	{
		
	}
}
