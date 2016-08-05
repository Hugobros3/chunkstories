package io.xol.chunkstories.world;

import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.world.io.IOTasks;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldClientLocal extends WorldClientCommon implements WorldMaster
{
	public WorldClientLocal(WorldInfo info)
	{
		super(info);
		
		ioHandler = new IOTasks(this);
		ioHandler.start();
	}

	@Override
	public Client getClient()
	{
		return Client.getInstance();
	}

	@Override
	public SoundManager getSoundManager()
	{
		//TODO when implementing server/client combo make sure we use something to mix behaviours of WorldServer and this
		return Client.getInstance().getSoundManager();
	}

	@Override
	public WorldRenderer getWorldRenderer()
	{
		return renderer;
	}
}
