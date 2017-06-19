package io.xol.chunkstories.api.client.net;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.world.WorldClient;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientPacketsProcessor extends PacketsProcessor {
	
	public ClientInterface getContext();
	
	public WorldClient getWorld();
	
	public Player getPlayer();
}
