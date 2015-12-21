package io.xol.chunkstories.entity;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public interface EntityControllable
{
	public Controller getController();
	
	public void controls(boolean focus);
}
