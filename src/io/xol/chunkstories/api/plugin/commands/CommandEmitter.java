package io.xol.chunkstories.api.plugin.commands;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Can represent the server console, a server player, the local client in case of local plugins, and so on...
 */
public interface CommandEmitter
{
	public String getName();

	public void sendMessage(String msg);

	public boolean hasPermission(String permissionNode);
}
