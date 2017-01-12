package io.xol.chunkstories.world.generator;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.exceptions.SyntaxErrorException;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.AssetHierarchy;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.content.GameContentStore;
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

public class WorldGeneratorsStore implements Content.WorldGenerators
{
	private final GameContentStore store;
	private final ModsManager modsManager;
	
	public WorldGeneratorsStore(GameContentStore store)
	{
		this.store = store;
		this.modsManager = store.modsManager();
		
		reload();
	}
	
	public Map<String, WorldGeneratorType> generators;
	public Map<Class<? extends WorldGenerator>, WorldGeneratorType> generatorsClasses = new HashMap<Class<? extends WorldGenerator>, WorldGeneratorType>();
	
	public class ActualWorldGeneratorType implements WorldGeneratorType {
		public ActualWorldGeneratorType(String name, Constructor<? extends WorldGenerator> constructor)
		{
			this.name = name;
			this.constructor = constructor;
		}

		private final String name;
		private final Constructor<? extends WorldGenerator> constructor;
		
		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public WorldGenerator instanciate()
		{
			try
			{
				return constructor.newInstance(new Object[] {});
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				e.printStackTrace();
			}
			
			ChunkStoriesLogger.getInstance().warning("Couldn't instanciate generator \"" + name + "\"; Instanciating BlankWorldGenerator instead.");
			return new BlankWorldGenerator();
		}
		
	}
	//public Map<String, Constructor<? extends WorldGenerator>> generators = new HashMap<String, Constructor<? extends WorldGenerator>>();
	//public Map<Class<? extends WorldGenerator>, String> generatorsClasses = new HashMap<Class<? extends WorldGenerator>, String>();

	WorldGeneratorType blank = new WorldGeneratorType() {

		@Override
		public String getName()
		{
			return "blank";
		}

		@Override
		public WorldGenerator instanciate()
		{
			return new BlankWorldGenerator();
		}
		
	};
	
	public void reload()
	{
		//Loads all generators
		generators.clear();
		generatorsClasses.clear();
		AssetHierarchy packetsFiles = modsManager.getAssetInstances("./data/worldGenerators.txt");
		
		Iterator<Asset> i = packetsFiles.iterator();
		while(i.hasNext())
		{
			Asset a = i.next();
			loadWorldGeneratorsFile(a);
		}
	}

	private void loadWorldGeneratorsFile(Asset a)
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

							ActualWorldGeneratorType generator = new ActualWorldGeneratorType(generatorName, constructor);
							
							generators.put(generatorName, generator);
							generatorsClasses.put(generatorClass, generator);
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

	public WorldGeneratorType getWorldGenerator(String name)
	{
		WorldGeneratorType generator = generators.get(name);
		if(generator != null)
			return generator;

		ChunkStoriesLogger.getInstance().warning("Couldn't find generator \"" + name + "\"; Providing BlankWorldGenerator instead.");
		return blank;
		/*if (generators.containsKey(name))
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
		
		
		return new BlankWorldGenerator();*/
	}
	
	public String getWorldGeneratorName(WorldGenerator generator)
	{
		String classname = generator.getClass().getName();
		if(generatorsClasses.containsKey(classname))
		{
			return generatorsClasses.get(classname).getName();
		}
		return "unknown";
	}

	@Override
	public Iterator<WorldGeneratorType> all()
	{
		return null;
	}

	@Override
	public Content parent()
	{
		return store;
	}
}
