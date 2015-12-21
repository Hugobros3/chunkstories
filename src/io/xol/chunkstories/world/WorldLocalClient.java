package io.xol.chunkstories.world;

import io.xol.chunkstories.world.io.IOTasks;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldLocalClient extends World
{

	public WorldLocalClient(WorldInfo info)
	{
		super(info);
		client = true;
		
		ioHandler = new IOTasks(this);
		ioHandler.start();
	}

}
