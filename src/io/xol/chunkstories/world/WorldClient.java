package io.xol.chunkstories.world;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

public class WorldClient extends World
{
	public WorldClient(WorldInfo info)
	{
		super(info);
		client = true;
		
		ioHandler = new IOTasksMultiplayerClient(this);
		ioHandler.start();
	}
}
