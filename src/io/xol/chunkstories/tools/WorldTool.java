package io.xol.chunkstories.tools;

import java.io.File;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.io.IOTasksImmediate;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldTool extends WorldImplementation implements WorldMaster
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

	@Override
	public SoundManager getSoundManager()
	{
		return null;
	}
}
