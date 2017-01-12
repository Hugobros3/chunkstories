package io.xol.chunkstories.particles;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.content.DefaultModsManager;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ParticleTypes
{
	private static Map<Integer, ParticleType> particleTypesById = new HashMap<Integer, ParticleType>();
	private static Map<String, ParticleType> particleTypesByName = new HashMap<String, ParticleType>();
	
	public static void reload()
	{
		particleTypesById.clear();
		particleTypesByName.clear();
		
		Iterator<Asset> i = DefaultModsManager.getAllAssetsByExtension("particles");
		while(i.hasNext())
		{
			Asset f = i.next();
			loadParticlesFile(f);
		}
	}

	private static void loadParticlesFile(Asset f)
	{
		if (f == null)
			return;
		try (BufferedReader reader = new BufferedReader(f.reader());)
		{
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					String splitted[] = line.split(" ");
					if (splitted.length == 3)
					{
						int id = Integer.parseInt(splitted[0]);
						String particleName = splitted[1];
						String className = splitted[2];

						try
						{
							Class<?> rawClass = DefaultModsManager.getClassByName(className);
							if (rawClass == null)
							{
								ChunkStoriesLogger.getInstance().warning("Particle class " + className + " does not exist in codebase.");
							}
							else if (!(ParticleType.class.isAssignableFrom(rawClass)))
							{
								ChunkStoriesLogger.getInstance().warning("Particle class " + className + " is not extending the ParticleType class.");
							}
							else
							{
								@SuppressWarnings("unchecked")
								Class<? extends ParticleType> itemClass = (Class<? extends ParticleType>) rawClass;
								Class<?>[] types = { Integer.TYPE, String.class };
								Constructor<? extends ParticleType> constructor = itemClass.getConstructor(types);
								
								if (constructor == null)
								{
									System.out.println("particle " + className + " does not provide a valid constructor.");
									continue;
								}
								
								ParticleType type = constructor.newInstance(new Object[] {id, particleName});
								particleTypesById.put(id, type);
								particleTypesByName.put(particleName, type);
								
								//System.out.println("Loaded particle "+type + "ok.");
							}

						}
						catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e)
						{
							e.printStackTrace();
						}
						
					}
				}
			}
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().warning(e.getMessage());
		}
	}

	public static ParticleType getParticleTypeByName(String string)
	{
		return particleTypesByName.get(string);
	}
	
	public static ParticleType getParticleTypeById(int id)
	{
		return particleTypesById.get(id);
	}
	
}
