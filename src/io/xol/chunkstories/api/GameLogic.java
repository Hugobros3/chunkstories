package io.xol.chunkstories.api;

import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.Scheduler;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface GameLogic
{
	public GameContext getGameContext();
	
	public int getTargetFps();
	
	public double getSimulationFps();
	
	public double getSimulationSpeed();
	
	public PluginManager getPluginsManager();

	public Scheduler getScheduler();
}