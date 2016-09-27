package io.xol.chunkstories.world.generator;

import io.xol.chunkstories.api.exceptions.SyntaxErrorException;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.content.Mods;
import io.xol.chunkstories.content.Mods.AssetHierarchy;
import io.xol.chunkstories.content.mods.Asset;
import io.xol.chunkstories.core.generator.BlankWorldGenerator;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldGenerators
{
	public static Map<String, Constructor<? extends WorldGenerator>> generators = new HashMap<String, Constructor<? extends WorldGenerator>>();
	public static Map<Class<? extends WorldGenerator>, String> generatorsClasses = new HashMap<Class<? extends WorldGenerator>, String>();

	public static void loadWorldGenerators()
	{
		//Loads all generators
		generators.clear();
		generatorsClasses.clear();
		AssetHierarchy packetsFiles = Mods.getAssetInstances("./data/worldGenerators.txt");
		
		Iterator<Asset> i = packetsFiles.iterator();
		while(i.hasNext())
		{
			Asset a = i.next();
			loadWorldGeneratorsFile(a);
		}
	}

	private static void loadWorldGeneratorsFile(Asset a)
	{
		if (a == null)
			return;
		try (BufferedReader reader = new BufferedReader(a.reader());)
		{
			String line = "";
			int ln = 0;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					String splitted[] = line.split(" ");
					if (splitted.length == 2)
					{
						String generatorName = splitted[0];
						try
						{
							Class<?> untypedClass = Class.forName(splitted[1]);
							if (!WorldGenerator.class.isAssignableFrom(untypedClass))
								throw new SyntaxErrorException(ln, a, splitted[1] + " is not a subclass of WorldGenerator");
							@SuppressWarnings("unchecked")
							Class<? extends WorldGenerator> generatorClass = (Class<? extends WorldGenerator>) untypedClass;

							Class<?>[] types = {};
							Constructor<? extends WorldGenerator> constructor = generatorClass.getConstructor(types);

							generators.put(generatorName, constructor);
							generatorsClasses.put(generatorClass, generatorName);
						}
						catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e)
						{
							e.printStackTrace();
						}
					}
				}

				ln++;
			}
		}
		catch (IOException | SyntaxErrorException e)
		{
			ChunkStoriesLogger.getInstance().warning(e.getMessage());
		}
	}

	public static WorldGenerator getWorldGenerator(String name)
	{
		if (generators.containsKey(name))
		{
			try
			{
				WorldGenerator generator = generators.get(name).newInstance(new Object[] {});
				return generator;
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}
		ChunkStoriesLogger.getInstance().warning("Couldn't find generator \"" + name + "\"; Using BlankGenerator instead.");
		return new BlankWorldGenerator();
	}
	
	public static String getWorldGeneratorName(WorldGenerator generator)
	{
		String classname = generator.getClass().getName();
		if(generatorsClasses.containsKey(classname))
		{
			return generatorsClasses.get(classname);
		}
		return "unknown";
	}
}
