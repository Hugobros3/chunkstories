package io.xol.chunkstories.api;

import io.xol.chunkstories.api.plugin.Scheduler;
import io.xol.chunkstories.content.PluginsManager;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface GameLogic
{
	public int getTargetFps();
	
	public double getSimulationFps();
	
	public double getSimulationSpeed();
	
	public default double getTickVelocityDivisor()
	{
		return getSimulationSpeed() / getTargetFps();
	}
	
	public PluginsManager getPluginsManager();

	public Scheduler getScheduler();
}