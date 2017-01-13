package io.xol.chunkstories.particles;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ParticlesTypesStore implements Content.ParticlesTypes
{
	private final GameContentStore store;
	private final ModsManager modsManager;
	
	public ParticlesTypesStore(GameContentStore store)
	{
		this.store = store;
		this.modsManager = store.modsManager();
		
		reload();
	}
	
	private Map<Integer, ParticleType> particleTypesById = new HashMap<Integer, ParticleType>();
	private Map<String, ParticleType> particleTypesByName = new HashMap<String, ParticleType>();
	
	public void reload()
	{
		particleTypesById.clear();
		particleTypesByName.clear();
		
		Iterator<Asset> i = modsManager.getAllAssetsByExtension("particles");
		while(i.hasNext())
		{
			Asset f = i.next();
			loadParticlesFile(f);
		}
	}

	private void loadParticlesFile(Asset f)
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
							Class<?> rawClass = modsManager.getClassByName(className);
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

	public ParticleType getParticleTypeByName(String string)
	{
		return particleTypesByName.get(string);
	}
	
	public ParticleType getParticleTypeById(int id)
	{
		return particleTypesById.get(id);
	}

	
	@Override
	public Iterator<ParticleType> all()
	{
		return this.particleTypesById.values().iterator();
	}

	@Override
	public Content parent()
	{
		// TODO Auto-generated method stub
		return store;
	}
	
}
