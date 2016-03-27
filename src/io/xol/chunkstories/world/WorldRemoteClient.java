package io.xol.chunkstories.world;

import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

public class WorldRemoteClient extends World implements WorldClient
{
	public WorldRemoteClient(WorldInfo info)
	{
		super(info);
		client = true;
		
		ioHandler = new IOTasksMultiplayerClient(this);
		ioHandler.start();
	}

	@Override
	public Client getClient()
	{
		return Client.getInstance();
	}
}
