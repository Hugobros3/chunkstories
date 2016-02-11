package io.xol.chunkstories.server.tech;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public interface CommandEmitter
{
	public String getName();

	public void sendMessage(String msg);

	public boolean hasPermission(String permissionNode);
}
