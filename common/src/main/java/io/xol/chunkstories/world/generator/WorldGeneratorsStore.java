//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.api.exceptions.content.IllegalWorldGeneratorDeclarationException;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.generator.BlankWorldGenerator;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.content.GenericNamedConfigurable;

public class WorldGeneratorsStore implements Content.WorldGenerators {
	private final GameContentStore store;
	private final ModsManager modsManager;

	private static final Logger logger = LoggerFactory.getLogger("content.generators");

	public Logger logger() {
		return logger;
	}

	public WorldGeneratorsStore(GameContentStore store) {
		this.store = store;
		this.modsManager = store.modsManager();

		// reload();
	}

	public Map<String, WorldGeneratorDefinition> generators = new HashMap<String, WorldGeneratorDefinition>();
	public Map<String, WorldGeneratorDefinition> generatorsClasses = new HashMap<String, WorldGeneratorDefinition>();

	public class ActualWorldGeneratorType extends GenericNamedConfigurable implements WorldGeneratorDefinition {

		public ActualWorldGeneratorType(String name, BufferedReader reader)
				throws IOException, IllegalWorldGeneratorDeclarationException {
			super(name, reader);

			try {
				className = this.resolveProperty("class", BlankWorldGenerator.class.getName());

				// System.out.println("Loading generator classname: "+className);
				Class<?> untypedClass = store.modsManager().getClassByName(className);

				// System.out.println("untypedClass="+untypedClass);
				// Class<?> untypedClass = Class.forName(className);
				if (!WorldGenerator.class.isAssignableFrom(untypedClass))
					throw new IllegalWorldGeneratorDeclarationException(
							className + " is not a subclass of WorldGenerator");
				@SuppressWarnings("unchecked")
				Class<? extends WorldGenerator> generatorClass = (Class<? extends WorldGenerator>) untypedClass;

				Class<?>[] types = { WorldGeneratorDefinition.class, World.class };

				constructor = generatorClass.getConstructor(types);
			} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
				e.printStackTrace();
				throw new IllegalWorldGeneratorDeclarationException(
						"WorldGenerator " + this.getName() + " has an issue with it's constructor: " + e.getMessage());
			}

			// System.out.println("GENERATOR TYPE INIT OK " + name);
		}

		private final String className;
		private final Constructor<? extends WorldGenerator> constructor;

		@Override
		public String getName() {
			return name;
		}

		@Override
		public WorldGenerator createForWorld(World world) {
			try {
				return constructor.newInstance(new Object[] { this, world });
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

			logger().warn(
					"Couldn't instanciate generator \"" + name + "\"; Instanciating BlankWorldGenerator instead.");

			// Return blank WG as a failover
			return new BlankWorldGenerator(this, world);
		}

		public String getGeneratorClassname() {
			return className;
		}

	}
	// public Map<String, Constructor<? extends WorldGenerator>> generators = new
	// HashMap<String, Constructor<? extends WorldGenerator>>();
	// public Map<Class<? extends WorldGenerator>, String> generatorsClasses = new
	// HashMap<Class<? extends WorldGenerator>, String>();

	/** Vanilla blank (void) world generator */
	WorldGeneratorDefinition blank = new WorldGeneratorDefinition() {

		@Override
		public String getName() {
			return "blank";
		}

		@Override
		public WorldGenerator createForWorld(World world) {
			return new BlankWorldGenerator(this, world);
		}

		@Override
		public String resolveProperty(String propertyName) {
			return null;
		}

		@Override
		public String resolveProperty(String propertyName, String defaultValue) {
			return defaultValue;
		}
	};

	public void reload() {
		// Loads all generators
		generators.clear();
		generatorsClasses.clear();

		/*
		 * AssetHierarchy packetsFiles =
		 * modsManager.getAssetInstances("./data/worldGenerators.txt");
		 * 
		 * Iterator<Asset> i = packetsFiles.iterator(); while(i.hasNext()) { Asset a =
		 * i.next(); loadWorldGeneratorsFile(a); }
		 */

		Iterator<Asset> i = modsManager.getAllAssetsByExtension("generators");
		while (i.hasNext()) {
			Asset f = i.next();
			logger().debug("Reading WorldGenerators declarations in : " + f);
			loadWorldGeneratorsFile(f);
		}
	}

	private void loadWorldGeneratorsFile(Asset a) {
		if (a == null)
			return;
		try (BufferedReader reader = new BufferedReader(a.reader());) {
			String line = "";
			@SuppressWarnings("unused")
			int ln = 0;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				} else {
					String splitted[] = line.split(" ");
					if (splitted.length == 2) {
						if (splitted[0].equals("generator")) {
							String name = splitted[1];

							try {
								ActualWorldGeneratorType generator = new ActualWorldGeneratorType(name, reader);

								generators.put(name, generator);

								generatorsClasses.put(generator.getGeneratorClassname(), generator);
							} catch (IOException | IllegalWorldGeneratorDeclarationException e) {
								e.printStackTrace();
							}
						}

						/*
						 * String generatorName = splitted[0]; try { Class<?> untypedClass =
						 * Class.forName(splitted[1]); if
						 * (!WorldGenerator.class.isAssignableFrom(untypedClass)) throw new
						 * SyntaxErrorException(ln, a, splitted[1] +
						 * " is not a subclass of WorldGenerator");
						 * 
						 * @SuppressWarnings("unchecked") Class<? extends WorldGenerator> generatorClass
						 * = (Class<? extends WorldGenerator>) untypedClass;
						 * 
						 * Class<?>[] types = {}; Constructor<? extends WorldGenerator> constructor =
						 * generatorClass.getConstructor(types);
						 * 
						 * ActualWorldGeneratorType generator = new
						 * ActualWorldGeneratorType(generatorName, constructor);
						 * 
						 * generators.put(generatorName, generator);
						 * generatorsClasses.put(generatorClass, generator); } catch
						 * (ClassNotFoundException | NoSuchMethodException | SecurityException |
						 * IllegalArgumentException e) { e.printStackTrace(); }
						 */
					}
				}

				ln++;
			}
		} catch (IOException e) {
			logger().warn(e.getMessage());
		}
	}

	public WorldGeneratorDefinition getWorldGenerator(String name) {
		WorldGeneratorDefinition generator = generators.get(name);
		if (generator != null)
			return generator;

		logger().warn("Couldn't find generator \"" + name + "\"; Providing BlankWorldGenerator instead.");
		return blank;
	}

	public WorldGeneratorDefinition getWorldGeneratorUnsafe(String name) {
		WorldGeneratorDefinition generator = generators.get(name);
		if (generator != null)
			return generator;

		return null;
	}

	public String getWorldGeneratorName(WorldGenerator generator) {
		String classname = generator.getClass().getName();
		if (generatorsClasses.containsKey(classname)) {
			return generatorsClasses.get(classname).getName();
		}
		return "unknown";
	}

	@Override
	public Iterator<WorldGeneratorDefinition> all() {
		return generators.values().iterator();
	}

	@Override
	public Content parent() {
		return store;
	}
}
