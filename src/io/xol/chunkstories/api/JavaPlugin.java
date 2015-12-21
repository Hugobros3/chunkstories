package io.xol.chunkstories.api;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class JavaPlugin
{
	protected ServerInterface server;
	
	public void setServer(ServerInterface server)
	{
		this.server = server;
	}
	
	public ServerInterface getServer()
	{
		return server;
	}
	
	public boolean handleCommand(Command cmd, String[] a, String rawText)
	{
		System.out.println("Someone left the default command handler !");
		return false;
	}
	
	public abstract void onEnable();
	public abstract void onDisable();
}
