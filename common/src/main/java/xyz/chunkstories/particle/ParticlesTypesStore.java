//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.particle;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.content.Content;
import xyz.chunkstories.api.content.mods.ModsManager;
import xyz.chunkstories.api.exceptions.content.IllegalParticleDeclarationException;
import xyz.chunkstories.api.particles.ParticleTypeHandler;
import xyz.chunkstories.content.GameContentStore;

public class ParticlesTypesStore implements Content.ParticlesTypes {
	private final GameContentStore store;
	private final ModsManager modsManager;

	private static final Logger logger = LoggerFactory.getLogger("content.particles");

	public Logger logger() {
		return logger;
	}

	public ParticlesTypesStore(GameContentStore store) {
		this.store = store;
		this.modsManager = store.modsManager();

		// reload();
	}

	// private Map<Integer, ParticleTypeHandler> particleTypesById = new
	// HashMap<Integer, ParticleTypeHandler>();
	private Map<String, ParticleTypeHandler> particleTypesByName = new HashMap<String, ParticleTypeHandler>();

	public void reload() {
		// particleTypesById.clear();
		particleTypesByName.clear();

		//TODO review this system as well
		/*Iterator<Asset> i = modsManager.getAllAssetsByExtension("particles");
		while (i.hasNext()) {
			Asset f = i.next();
			loadParticlesFile(f);
		}*/
	}

	private void loadParticlesFile(Asset f) {
		if (f == null)
			return;
		throw new UnsupportedOperationException("TODO");
		/*try (BufferedReader reader = new BufferedReader(f.reader());) {
			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				} else {
					if (line.startsWith("end")) {
						logger().warn("Syntax error in file : " + f + " : ");
						continue;
					}
					String splitted[] = line.split(" ");
					if (splitted.length == 2 && splitted[0].startsWith("particle")) {
						// int id = Integer.parseInt(splitted[2]);
						String particleName = splitted[1];

						try {
							ParticleTypeDefinitionImplementation type = new ParticleTypeDefinitionImplementation(this,
									particleName, reader);
							ParticleTypeHandler handler = type.handler();

							particleTypesByName.put(particleName, handler);
							// particleTypesById.put(id, handler);

						} catch (IllegalParticleDeclarationException e) {
							this.store.getContext().logger()
									.error("Could not load particle type " + particleName + " : \n" + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
		} catch (IOException e) {
			logger().warn(e.getMessage());
		}*/
	}

	@Override
	public ParticleTypeHandler getParticleType(String string) {
		return particleTypesByName.get(string);
	}

	/*
	 * @Override public ParticleTypeHandler getParticleTypeHandlerById(int id) {
	 * return particleTypesById.getVoxelComponent(id); }
	 */

	@Override
	public Iterator<ParticleTypeHandler> all() {
		return this.particleTypesByName.values().iterator();
	}

	@Override
	public Content parent() {
		return store;
	}

}
