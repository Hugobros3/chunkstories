package io.xol.chunkstories.api;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.exceptions.PluginInfoException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PluginStore
{
	ServerInterface server;
	
	Set<PluginJar> loadedPlugins = new HashSet<PluginJar>();
	
	public Set<PluginJar> getLoadedPlugins()
	{
		return loadedPlugins;
	}
	
	public void loadPlugins(File pluginsDir, boolean reload)
	{
		if(reload)
		{
			for(PluginJar pj : loadedPlugins)
				unLoadPlugin(pj);
		}
		if(pluginsDir.isDirectory() && pluginsDir.exists())
		{
			for(File f : pluginsDir.listFiles())
				if(!f.isDirectory() && f.getName().endsWith(".jar"))
					loadPlugin(f);
		}
	}

	public void loadPlugin(File file)
	{
		try
		{
			PluginJar plugin = new PluginJar(file, getClass().getClassLoader());
			loadedPlugins.add(plugin);
		}
		catch (PluginInfoException | IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void unLoadPlugin(PluginJar pj)
	{
		//TODO ???
		loadedPlugins.remove(pj);
	}
}
