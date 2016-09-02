package io.xol.chunkstories.api.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.xol.chunkstories.api.exceptions.PluginInfoException;
import io.xol.chunkstories.api.plugin.commands.Command;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PluginInformation extends URLClassLoader
{
	public PluginInformation(File file, ClassLoader parentLoader) throws PluginInfoException, IOException
	{
		super(new URL[] { file.toURI().toURL() }, parentLoader);
		jar = new JarFile(file);
		assert parentLoader != null;
		loadInformation(getRessourceFromJar(file, "plugin.info"));
		// TODO Load ressources ?
		jar.close();
	}

	public String getName()
	{
		return pluginName;
	}

	String pluginName;
	String authors;
	float pluginVersion;
	String entryPoint;

	// Commands handled by this plugin
	public Set<Command> commands = new HashSet<Command>();

	JarFile jar = null;

	private void loadInformation(InputStream inputStream) throws PluginInfoException, IOException
	{
		if (inputStream == null)
			throw new PluginInfoException();

		commands.clear();

		InputStreamReader ipsr = new InputStreamReader(inputStream, "UTF-8");
		BufferedReader br = new BufferedReader(ipsr);
		String line;
		while ((line = br.readLine()) != null)
		{
			if (!line.startsWith("#"))
			{
				if (line.contains(": "))
				{
					String[] s = line.split(": ");
					String variable = s[0];
					String value = s[1];
					switch (variable)
					{
					default:
						System.out.println("Unknown plugin variable : " + variable);
						break;
					case "name":
						pluginName = value;
						break;
					case "authors":
						authors = value;
						break;
					case "version":
						pluginVersion = Float.parseFloat(value);
						break;
					case "main":
						entryPoint = value;
						break;
					case "command":
						String[] aliases = value.split(" ");
						Command command = new Command(aliases[0]);
						
						for(int i = 1; i < aliases.length; i++)
							command.addAlias(aliases[i]);
						
						commands.add(command);
						break;
					}
				}
			}
		}
		System.out.println("Loaded info : " + pluginName + " version " + pluginVersion + " by " + authors + " EP:" + entryPoint);
		br.close();
	}

	public float getPluginVersion()
	{
		return pluginVersion;
	}

	public ChunkStoriesPlugin createInstance(PluginManager pluginManager)
	{
		try
		{
			Class<?> entryPointClass = Class.forName(entryPoint, true, this);
			Class<? extends ChunkStoriesPlugin> javaPluginClass = entryPointClass.asSubclass(ChunkStoriesPlugin.class);
			ChunkStoriesPlugin plugin = javaPluginClass.newInstance();
			plugin.initialize(pluginManager, this);
			return plugin;
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
		{
			System.out.println("Unable to find and initiliaze plugin : " + pluginName + " from file : Missing class '" + entryPoint + "'");
			e.printStackTrace();
		}
		return null;
	}

	public InputStream getRessourceFromJar(File file, String ressource)
	{
		try
		{
			JarEntry entry = jar.getJarEntry(ressource);
			return jar.getInputStream(entry);
		}
		catch (Exception e)
		{
			System.out.println("Unable to get ressource '" + ressource + "' from jar file '" + file.getAbsolutePath() + "'");
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		System.out.println(name);
		return super.findClass(name);
	}
}
