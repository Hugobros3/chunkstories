package io.xol.chunkstories.world.generator;

import io.xol.chunkstories.GameData;
import io.xol.chunkstories.api.exceptions.SyntaxErrorException;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.generator.core.BlankWorldGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldGenerators
{
	public static Map<String, Constructor<? extends WorldGenerator>> generators = new HashMap<String, Constructor<? extends WorldGenerator>>();

	public static void loadWorldGenerators()
	{
		//Loads all generators
		generators.clear();
		Deque<File> packetsFiles = GameData.getAllFileInstances("./res/data/worldGenerators.txt");
		for (File f : packetsFiles)
		{
			loadWorldGeneratorsFile(f);
		}
	}

	private static void loadWorldGeneratorsFile(File f)
	{
		if (!f.exists())
			return;
		try (FileReader fileReader = new FileReader(f); BufferedReader reader = new BufferedReader(fileReader);)
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
								throw new SyntaxErrorException(ln, f, splitted[1] + " is not a subclass of WorldGenerator");
							@SuppressWarnings("unchecked")
							Class<? extends WorldGenerator> generatorClass = (Class<? extends WorldGenerator>) untypedClass;

							Class<?>[] types = {};
							Constructor<? extends WorldGenerator> constructor = generatorClass.getConstructor(types);

							generators.put(generatorName, constructor);
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
}
