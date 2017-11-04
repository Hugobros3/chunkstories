package io.xol.chunkstories.particles;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.exceptions.content.IllegalParticleDeclarationException;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

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
		
		//reload();
	}
	
	private Map<Integer, ParticleTypeHandler> particleTypesById = new HashMap<Integer, ParticleTypeHandler>();
	private Map<String, ParticleTypeHandler> particleTypesByName = new HashMap<String, ParticleTypeHandler>();
	
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
					if (line.startsWith("end"))
					{
						ChunkStoriesLoggerImplementation.getInstance().warning("Syntax error in file : " + f + " : ");
						continue;
					}
					String splitted[] = line.split(" ");
					if (splitted.length == 3 && splitted[0].startsWith("particle"))
					{
						int id = Integer.parseInt(splitted[2]);
						String particleName = splitted[1];
						
						try {
							ParticleTypeImpl type = new ParticleTypeImpl(this, particleName, id, reader);
							ParticleTypeHandler handler = type.handler();
							
							particleTypesByName.put(particleName, handler);
							particleTypesById.put(id, handler);
							
						} catch (IllegalParticleDeclarationException e) {
							this.store.getContext().logger().error("Could not load particle type "+particleName+" : \n"+e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			ChunkStoriesLoggerImplementation.getInstance().warning(e.getMessage());
		}
	}

	@Override
	public ParticleTypeHandler getParticleTypeHandlerByName(String string)
	{
		return particleTypesByName.get(string);
	}
	
	@Override
	public ParticleTypeHandler getParticleTypeHandlerById(int id)
	{
		return particleTypesById.get(id);
	}
	
	@Override
	public Iterator<ParticleTypeHandler> all()
	{
		return this.particleTypesById.values().iterator();
	}

	@Override
	public Content parent()
	{
		return store;
	}
	
}
