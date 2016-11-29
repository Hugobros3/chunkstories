package io.xol.chunkstories.api.world;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public enum WorldAuthority
{
	CLIENT_ONLY(true, false), // Is only client, not master
	CLIENT_LOCALHOST(true, true), // Is both
	SERVER(false, true), // Is only master
	NONE(false, false); // Unused
	
	private final boolean isClient, isMaster;
	
	private WorldAuthority(boolean isClient, boolean isMaster)
	{
		this.isClient = isClient;
		this.isMaster = isMaster;
	}
	
	public boolean isClient()
	{
		return isClient;
	}
	
	public boolean isMaster()
	{
		return isMaster;
	}
}
