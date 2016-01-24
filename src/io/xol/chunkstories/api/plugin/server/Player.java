package io.xol.chunkstories.api.plugin.server;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Player
{
	public String getName();
	public Entity getControlledEntity();
	
	public void sendTextMessage(String msg);
	
	public Location getPosition();
	public void setPosition(Location l);
	
	public boolean isConnected();
}
