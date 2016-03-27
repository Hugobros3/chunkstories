package io.xol.chunkstories.api.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.exceptions.PluginInfoException;
import io.xol.chunkstories.api.server.ServerInterface;

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
	
	/**
	 * Loads all .jar files found within a directory
	 * @param pluginsDir
	 * @param reload
	 */
	public void loadPlugins(File pluginsDir)
	{
		if(pluginsDir.isDirectory() && pluginsDir.exists())
		{
			for(File f : pluginsDir.listFiles())
				if(!f.isDirectory() && f.getName().endsWith(".jar"))
					loadPlugin(f);
		}
	}

	/**
	 * Loads a plugin
	 * @param file The file containing the plugin
	 */
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
	
	/**
	 * Removes a plugin
	 * @param pj
	 */
	public void unLoadPlugin(PluginJar pj)
	{
		loadedPlugins.remove(pj);
	}

	public void unloadPlugins()
	{
		loadedPlugins.clear();
	}
}
