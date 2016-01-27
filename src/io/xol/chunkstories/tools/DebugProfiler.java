package io.xol.chunkstories.tools;

import java.util.LinkedList;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class DebugProfiler
{

	// Quick & Dirty profiler for the game

	public long lastReset = 0;
	public long currentSection = 0;
	public String currentSectionName = "default";

	//public String profiling = "";

	class ProfileSection
	{
		String name;
		long timeTookNs;

		public ProfileSection(String name, long timeTookNs)
		{
			this.name = name;
			this.timeTookNs = timeTookNs;
		}

		public String toString()
		{
			return "[" + name + ":" + Math.floor(timeTookNs / 10000) / 100.0f + "]";
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof ProfileSection)
				return ((ProfileSection) obj).name.equals(name);
			return super.equals(obj);
		}

		@Override
		public int hashCode()
		{
			return name.hashCode();
		}
	}

	public LinkedList<ProfileSection> sections = new LinkedList<ProfileSection>();

	public String reset(String name)
	{
		//Timings
		long took = System.nanoTime() - currentSection;

		//Reset timers
		currentSection = System.nanoTime();
		lastReset = System.nanoTime();

		//Build string and reset sections
		String txt = "";
		for (ProfileSection section : sections)
		{
			txt += section + " ";
		}
		sections.clear();
		//Add new section
		ProfileSection section = new ProfileSection(name, took);
		sections.add(section);

		return txt;
	}

	public String reset()
	{
		return reset("default");
	}

	public void startSection(String name)
	{
		long took = System.nanoTime() - currentSection;
		ProfileSection section = new ProfileSection(name, took);
		sections.add(section);
		currentSection = System.nanoTime();
		currentSectionName = name;
	}

	public int timeTook()
	{
		return (int) (System.nanoTime() - lastReset) / 1000000;
	}
}
