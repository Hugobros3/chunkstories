package io.xol.chunkstories.api;

import io.xol.chunkstories.api.plugin.Scheduler;
import io.xol.chunkstories.content.DefaultPluginManager;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface GameLogic
{
	public int getTargetFps();
	
	public double getSimulationFps();
	
	public double getSimulationSpeed();
	
	public DefaultPluginManager getPluginsManager();

	public Scheduler getScheduler();
}