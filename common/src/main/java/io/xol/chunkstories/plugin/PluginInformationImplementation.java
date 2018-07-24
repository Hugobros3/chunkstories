//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.exceptions.plugins.PluginCreationException;
import io.xol.chunkstories.api.exceptions.plugins.PluginInfoException;
import io.xol.chunkstories.api.exceptions.plugins.PluginLoadException;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.ClientPlugin;
import io.xol.chunkstories.api.plugin.PluginInformation;
import io.xol.chunkstories.api.plugin.ServerPlugin;
import io.xol.chunkstories.api.plugin.commands.PluginCommand;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.content.GameDirectory;

/**
 * Loads the plugin definition from it's Jar file and allows to instanciate it
 */
public class PluginInformationImplementation extends URLClassLoader implements PluginInformation {
	private String pluginName;
	private String authors;
	private String pluginVersion;
	private String entryPointClassName;

	// Commands handled by this plugin
	private final Set<PluginCommand> commands = new HashSet<PluginCommand>();

	private final PluginType pluginType;

	private final Class<? extends ChunkStoriesPlugin> entryPointClass;
	private final Constructor<? extends ChunkStoriesPlugin> entryPointConstructor;

	@SuppressWarnings("serial")
	public PluginInformationImplementation(File file, ClassLoader parentLoader)
			throws PluginLoadException, IOException {
		super(new URL[] { file.toURI().toURL() }, parentLoader);

		// Opens the jar and grabs plugin.info
		JarFile jar = new JarFile(file);

		InputStream pluginInfo = getRessourceFromJar(jar, "plugin.info");

		if (pluginInfo == null)
			throw new NotAPluginException(file);

		loadInformation(pluginInfo);
		jar.close();

		try {
			Class<?> entryPointClassUnchecked = Class.forName(entryPointClassName, true, this);

			// Checks for class fitness as an entry point
			if (!ChunkStoriesPlugin.class.isAssignableFrom(entryPointClassUnchecked))
				throw new PluginLoadException() {
					public String getMessage() {
						return "Entry point not implementing ChunkStoriesPlugin or a subtype :" + entryPointClassName;
					}
				};

			// Casts
			entryPointClass = entryPointClassUnchecked.asSubclass(ChunkStoriesPlugin.class);

			// Determines the plugin type and obtains the suitable constructor
			if (ClientPlugin.class.isAssignableFrom(entryPointClassUnchecked)) {
				pluginType = PluginType.CLIENT_ONLY;

				Class<?>[] types = new Class[] { PluginInformation.class, ClientInterface.class };
				entryPointConstructor = entryPointClass.getConstructor(types);
			} else if (ServerPlugin.class.isAssignableFrom(entryPointClassUnchecked)) {
				pluginType = PluginType.MASTER;

				Class<?>[] types = new Class[] { PluginInformation.class, ServerInterface.class };
				entryPointConstructor = entryPointClass.getConstructor(types);
			}
			// If it's not a derivative of either ClientPlugin or ServerPlugin, it's then a
			// UniversalPlugin
			else {
				pluginType = PluginType.UNIVERSAL;

				Class<?>[] types = new Class[] { PluginInformation.class, GameContext.class };
				entryPointConstructor = entryPointClass.getConstructor(types);
			}

		} catch (ClassNotFoundException e) {
			throw new PluginLoadException() {
				public String getMessage() {
					return "Entry point class not found :" + entryPointClassName;
				}
			};
		} catch (NoSuchMethodException | SecurityException e) {
			throw new PluginLoadException() {
				public String getMessage() {
					return "Suitable constructor for plugin type " + pluginType + " not found in class :"
							+ entryPointClassName;
				}
			};
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.chunkstories.plugin.PluginInformation#getName()
	 */
	@Override
	public String getName() {
		return this.pluginName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.chunkstories.plugin.PluginInformation#getPluginVersion()
	 */
	@Override
	public String getPluginVersion() {
		return this.pluginVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.chunkstories.plugin.PluginInformation#getAuthor()
	 */
	@Override
	public String getAuthor() {
		return this.authors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.chunkstories.plugin.PluginInformation#getPluginType()
	 */
	@Override
	public PluginType getPluginType() {
		return pluginType;
	}

	private void loadInformation(InputStream inputStream) throws PluginInfoException, IOException {
		if (inputStream == null)
			throw new PluginInfoException();

		InputStreamReader ipsr = new InputStreamReader(inputStream, "UTF-8");
		BufferedReader br = new BufferedReader(ipsr);
		String line;
		while ((line = br.readLine()) != null) {
			if (!line.startsWith("#")) {
				if (line.contains(": ")) {
					String[] s = line.split(": ");
					String variable = s[0];
					String value = s[1];
					switch (variable) {
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
						pluginVersion = value;
						break;
					case "main":
						entryPointClassName = value;
						break;
					case "command":
						String[] aliases = value.split(" ");
						PluginCommand command = new PluginCommand(this, aliases[0]);

						for (int i = 1; i < aliases.length; i++)
							command.addAlias(aliases[i]);

						commands.add(command);
						break;
					}
				}
			}
		}

		// System.out.println("Loaded info : " + pluginName + " version " +
		// pluginVersion + " by " + authors + " EP:" + entryPoint);
		br.close();
	}

	@Override
	public String toString() {
		return "[Plugin " + this.pluginType + " | " + this.pluginName + " " + this.pluginVersion + " by " + this.authors
				+ "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.xol.chunkstories.plugin.PluginInformation#createInstance(io.xol.
	 * chunkstories.api.GameContext)
	 */
	@Override
	@SuppressWarnings("serial")
	public ChunkStoriesPlugin createInstance(GameContext pluginExecutionContext) throws PluginCreationException {
		try {
			switch (pluginType) {
			case CLIENT_ONLY:
				if (!(pluginExecutionContext instanceof ClientInterface))
					throw new IllegalArgumentException() {
						public String getMessage() {
							return "Attempted to create a clientside-only plugin without using a ClientInterface as a PluginExecutionContext";
						}
					};

				ClientInterface clientInterface = (ClientInterface) pluginExecutionContext;
				return (ClientPlugin) entryPointConstructor.newInstance(new Object[] { this, clientInterface });
			case MASTER:
				if (!(pluginExecutionContext instanceof ServerInterface))
					throw new IllegalArgumentException() {
						public String getMessage() {
							return "Attempted to create a serverside-only plugin without using a ServerInterface as a PluginExecutionContext";
						}
					};

				ServerInterface serverInterface = (ServerInterface) pluginExecutionContext;
				return (ServerPlugin) entryPointConstructor.newInstance(new Object[] { this, serverInterface });
			default:
				return (ChunkStoriesPlugin) entryPointConstructor
						.newInstance(new Object[] { this, pluginExecutionContext });
			}
		}
		// Catch-all for plugin creation failure
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			throw new PluginCreationException() {
				public String getMessage() {
					return "Failed to call constructor for:" + entryPointClassName;
				}
			};
		}
	}

	private InputStream getRessourceFromJar(JarFile jar, String ressource) {
		try {
			JarEntry entry = jar.getJarEntry(ressource);
			return jar.getInputStream(entry);
		} catch (Exception e) {
			// System.out.println("Unable to get ressource '" + ressource + "' from jar '" +
			// jar + "'");
			// e.printStackTrace();
		}
		return null;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// System.out.println("Looking for class "+name + " in plugin "+jar);
		return super.findClass(name);
	}

	@Override
	public Collection<PluginCommand> getCommands() {
		return commands;
	}

	@Override
	public File getDirectory() {

		File file = new File(GameDirectory.getGameFolderPath() + "/" + this.getName() + "/");
		file.mkdirs();

		return file;
	}
}
