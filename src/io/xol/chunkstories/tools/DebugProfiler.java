package io.xol.chunkstories.tools;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class DebugProfiler
{

	// Quick & Dirty profiler for the game

	public long lastReset = 0;
	public long currentSection = 0;
	public String currentSectionName = "default";

	public String profiling = "";

	public String reset(String name)
	{
		long took = System.currentTimeMillis() - currentSection;
		profiling += currentSectionName + " " + took + " ms; ";
		String profiling2 = profiling;
		profiling = "";
		currentSection = System.currentTimeMillis();
		currentSectionName = name;
		lastReset = System.currentTimeMillis();
		return profiling2;
	}

	public String reset()
	{
		return reset("default");
	}

	public void startSection(String name)
	{
		long took = System.currentTimeMillis() - currentSection;
		profiling += currentSectionName + " " + took + " ms; ";
		currentSection = System.currentTimeMillis();
		currentSectionName = name;
	}

	public int timeTook()
	{
		return (int) (System.currentTimeMillis() - lastReset);
	}
}
