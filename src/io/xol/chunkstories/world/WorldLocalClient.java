package io.xol.chunkstories.world;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.io.IOTasks;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldLocalClient extends World implements WorldClient, WorldMaster
{
	public WorldLocalClient(WorldInfo info)
	{
		super(info);
		client = true;
		
		ioHandler = new IOTasks(this);
		ioHandler.start();
	}

	@Override
	public Client getClient()
	{
		return Client.getInstance();
	}

	@Override
	public void playSoundEffectExcluding(String soundEffect, Location location, float pitch, float gain, Subscriber subscriber)
	{
		
	}
}
