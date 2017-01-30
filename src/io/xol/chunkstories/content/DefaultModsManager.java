package io.xol.chunkstories.content;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.xol.chunkstories.api.exceptions.plugins.PluginLoadException;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.AssetHierarchy;
import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.content.mods.ForeignCodeClassLoader;
import io.xol.chunkstories.content.mods.ModImplementation;
import io.xol.chunkstories.content.mods.ModFolder;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;
import io.xol.chunkstories.content.mods.exceptions.ModNotFoundException;
import io.xol.chunkstories.content.mods.exceptions.NotAllModsLoadedException;
import io.xol.chunkstories.plugin.NotAPluginException;
import io.xol.chunkstories.plugin.PluginInformationImplementation;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.ChunkStoriesLogger.LogLevel;
import io.xol.engine.concurrency.UniqueList;
import io.xol.engine.misc.FoldersUtils;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DefaultModsManager implements ModsManager
{
	private Mod baseAssets;
	private String[] modsEnabled = new String[0];
	private UniqueList<Mod> enabledMods = new UniqueList<Mod>();
	private Map<String, ModsAssetHierarchy> avaibleAssets = new HashMap<String, ModsAssetHierarchy>();
	private Map<String, ForeignCodeClassLoader> avaibleForeignClasses = new HashMap<String, ForeignCodeClassLoader>();

	private File cacheFolder = null;
	private List<PluginInformationImplementation> pluginsWithinEnabledMods = new ArrayList<PluginInformationImplementation>();

	public DefaultModsManager()
	{
		this(null);
	}
	
	public DefaultModsManager(String enabledModsAtStart)
	{
		if(enabledModsAtStart != null)
			modsEnabled = enabledModsAtStart.split(",");
	}

	public static void main(String a[])
	{
		ModsManager mm = new DefaultModsManager();
		
		try
		{
			mm.setEnabledMods("dogez_content");
			//setEnabledMods("C:\\Users\\Hugo\\workspace2\\Dogez-Plugin for CS\\mods\\dogez_content", "modInZip", "OveriddenModInZip", "md5:df9f7c813fdc72029b41758ef8dbb528", "md5:7f46165474d11ee5836777d85df2cdab:http://xol.io");
			mm.loadEnabledMods();
		}
		catch (NotAllModsLoadedException e)
		{
			System.out.print(e.getMessage());
		}
				
		for(Mod mod : mm.getCurrentlyLoadedMods())
			System.out.println(mod.getMD5Hash());
			
		try
		{
			mm.setEnabledMods("dogez_content2");
			//setEnabledMods("C:\\Users\\Hugo\\workspace2\\Dogez-Plugin for CS\\mods\\dogez_content", "modInZip", "OveriddenModInZip", "md5:df9f7c813fdc72029b41758ef8dbb528", "md5:7f46165474d11ee5836777d85df2cdab:http://xol.io");
			mm.loadEnabledMods();
		}
		catch (NotAllModsLoadedException e)
		{
			System.out.print(e.getMessage());
		}
				
		for(Mod mod : mm.getCurrentlyLoadedMods())
			System.out.println(mod.getMD5Hash());
		
		try
		{
			mm.setEnabledMods("dogez_content");
			//setEnabledMods("C:\\Users\\Hugo\\workspace2\\Dogez-Plugin for CS\\mods\\dogez_content", "modInZip", "OveriddenModInZip", "md5:df9f7c813fdc72029b41758ef8dbb528", "md5:7f46165474d11ee5836777d85df2cdab:http://xol.io");
			mm.loadEnabledMods();
		}
		catch (NotAllModsLoadedException e)
		{
			System.out.print(e.getMessage());
		}
				
		for(Mod mod : mm.getCurrentlyLoadedMods())
			System.out.println(mod.getMD5Hash());
		
		System.out.println("Done");
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#setEnabledMods(java.lang.String)
	 */
	@Override
	public void setEnabledMods(String... modsEnabled)
	{
		this.modsEnabled = modsEnabled;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#loadEnabledMods()
	 */
	@Override
	public void loadEnabledMods() throws NotAllModsLoadedException
	{
		enabledMods.clear();
		List<ModLoadFailureException> modLoadExceptions = new ArrayList<ModLoadFailureException>();

		//Creates mods dir if it needs to
		File modsDir = new File(GameDirectory.getGameFolderPath() + "/mods");
		if (!modsDir.exists())
			modsDir.mkdirs();
		
		//Server mods
		File serverMods = new File(GameDirectory.getGameFolderPath() + "/servermods");
		if (!serverMods.exists())
			serverMods.mkdirs();

		for (String name : modsEnabled)
		{
			try
			{
				ModImplementation mod = null;

				//Servers give a md5 hash for their required mods
				if (name.startsWith("md5:"))
				{
					//Look for a mod with that md5 hash
					String hash = name.substring(4, name.length());
					String url = null;
					//If the hash is bundled with an url, split'em
					if (hash.contains(":"))
					{
						int i = hash.indexOf(":");
						url = hash.substring(i + 1);
						hash = hash.substring(0, i);
					}
					System.out.println("Looking for hashed mod " + hash + " (url = " + url + ")");

					//Look for the mod zip in local fs first.
					File zippedMod = new File(serverMods.getAbsolutePath() + "/" + hash + ".zip");
					if (zippedMod.exists())
					{
						//Awesome we found it !
						mod = new ModZip(zippedMod);
					}
					else if (url != null)
					{
						//TODO download and hanle files from server
					}
					else
					{
						//We failed. Mod won't be loaded
					}
				}
				else
				{
					System.out.println("Looking for mod " + name + " on the local filesystem");

					//First look for it in the directory section
					File modDirectory = new File(modsDir.getAbsolutePath() + "/" + name);
					if (modDirectory.exists())
					{
						mod = new ModFolder(modDirectory);
						System.out.println("Found mod in directory : " + modDirectory);
					}
					else
					{
						//Then look for a .zip file in the same directory
						File zippedMod = new File(modsDir.getAbsolutePath() + "/" + name + ".zip");
						if (zippedMod.exists())
						{
							mod = new ModZip(zippedMod);
							System.out.println("Found mod in zipfile : " + zippedMod);
						}
						else
						{
							//Finally just look for it in the global os path
							if (name.endsWith(".zip"))
							{
								zippedMod = new File(name);
								if (zippedMod.exists())
								{
									mod = new ModZip(zippedMod);
									System.out.println("Found mod in global zipfile : " + zippedMod);
								}
							}
							else
							{
								modDirectory = new File(name);
								if (modDirectory.exists())
								{
									mod = new ModFolder(modDirectory);
									System.out.println("Found mod in global directory : " + modDirectory);
								}
							}
						}
					}
				}

				//Did we manage it ?
				if (mod != null)
				{
					if (!enabledMods.add(mod))
					{
						//Somehow we added a mod twice and it's now conflicting.
						throw new ModLoadFailureException(mod, "Conflicting mod, another mod with the same name or hash is already loaded.");
					}
				}
				else
					throw new ModNotFoundException(name);
			}
			catch (ModLoadFailureException exception)
			{
				modLoadExceptions.add(exception);
			}
		}

		buildModsFileSystem();

		//Return an exception if some mods failed to load.
		if (modLoadExceptions.size() > 0)
			throw new NotAllModsLoadedException(modLoadExceptions);
	}

	private void buildModsFileSystem()
	{
		avaibleAssets.clear();
		avaibleForeignClasses.clear();
		
		pluginsWithinEnabledMods.clear();
		
		//Obtain a cache folder
		if (cacheFolder == null)
		{
			cacheFolder = new File(GameDirectory.getGameFolderPath() + "/cache/" + ((int) (Math.random() * 10000)));
			cacheFolder.mkdirs();
			//cacheFolder.deleteOnExit();
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				public void run()
				{
					System.out.println("Deleting cache folder " + cacheFolder);
					FoldersUtils.deleteFolder(cacheFolder);
				}
			});
		}

		// Checks for the base assets folder presence and sanity
		try
		{
			baseAssets = new ModFolder(new File(GameDirectory.getGameFolderPath() + "/res/"));
		}
		catch (ModLoadFailureException e)
		{
			ChunkStoriesLogger.getInstance().error("Fatal : failed to load in the base assets folder. Exception :");
			e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
		}

		//Iterates over mods, in order of priority
		for (Mod mod : enabledMods)
		{
			loadModAssets(mod);
		}
		//Just loads the rest
		loadModAssets(baseAssets);
	}

	private void loadModAssets(Mod mod)
	{
		//For each asset in the said mod
		for (Asset asset : mod.assets())
		{
			//Skips mod.txt
			if (asset.getName().equals("./mod.txt"))
				continue;

			//Special case for .jar files : we extract them in the cache/ folder and make them avaible through secure ClassLoaders
			if (asset.getName().endsWith(".jar"))
			{
				loadJarFile(asset);
				continue;
			}

			//Look for it's entry
			ModsAssetHierarchy entry = avaibleAssets.get(asset.getName());
			if (entry == null)
			{
				entry = new ModsAssetHierarchy(asset);
				avaibleAssets.put(asset.getName(), entry);
			}
			else
			{
				System.out.println("Adding asset " + asset + " but it's already overriden ! (top=" + entry.topInstance() + ")");
				entry.addAssetInstance(asset);
			}
		}
	}

	private void loadJarFile(Asset asset)
	{
		System.out.println("Handling jar file " + asset);
		try
		{
			//Read the jar file contents and extract it somewhere on cache
			//TODO hash dat crap this boi, the collision probs!!!
			int random = ((int) (Math.random() * 16384960));
			File cachedJarLocation = new File(cacheFolder.getAbsolutePath() + "/" + random + ".jar");
			FileOutputStream fos = new FileOutputStream(cachedJarLocation);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			InputStream is = asset.read();
			System.out.println("Writing to " + cachedJarLocation);
			byte[] buf = new byte[4096];
			while (is.available() > 0)
			{
				int read = is.read(buf);
				bos.write(buf, 0, read);
				if (read == 0)
					break;
			}
			bos.flush();
			bos.close();
			System.out.println("Done writing file");

			//Create a fancy class loader for this temp jar
			ForeignCodeClassLoader classLoader = new ForeignCodeClassLoader(asset.getSource(), cachedJarLocation, Thread.currentThread().getContextClassLoader());
			
			for(String className : classLoader.classes())
			{
				//System.out.println("class "+className+" found in jar "+asset);
				avaibleForeignClasses.put(className, classLoader);
			}
			
			//Checks if it may load as a plugin
			try
			{
				PluginInformationImplementation pluginInformation = new PluginInformationImplementation(cachedJarLocation, PluginInformationImplementation.class.getClassLoader());
				System.out.println("Found plugin "+pluginInformation+" from within "+asset.getSource());
				pluginsWithinEnabledMods.add(pluginInformation);
			}
			catch (NotAPluginException nap)
			{
				//Discard silently
			}
			catch (PluginLoadException e)
			{
				System.out.println("Something went wrong loading the plugin @ "+asset);
				e.printStackTrace();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public Iterator<ModsAssetHierarchy> getAllUniqueEntries()
	{
		return avaibleAssets.values().iterator();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getAllUniqueFilesLocations()
	 */
	@Override
	public Iterator<Asset> getAllUniqueFilesLocations()
	{
		return new Iterator<Asset>()
		{
			Iterator<ModsAssetHierarchy> i = getAllUniqueEntries();

			@Override
			public boolean hasNext()
			{
				return i.hasNext();
			}

			@Override
			public Asset next()
			{
				return i.next().topInstance();
			}

		};
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getAsset(java.lang.String)
	 */
	@Override
	public Asset getAsset(String assetName)
	{
		AssetHierarchy asset = avaibleAssets.get(assetName);
		if(asset == null)
			return null;
		
		return asset.topInstance();
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getAssetInstances(java.lang.String)
	 */
	@Override
	public ModsAssetHierarchy getAssetInstances(String assetName)
	{
		return avaibleAssets.get(assetName);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getAllAssetsByExtension(java.lang.String)
	 */
	@Override
	public Iterator<Asset> getAllAssetsByExtension(String extension)
	{
		return new Iterator<Asset>() {

			Iterator<ModsAssetHierarchy> base = getAllUniqueEntries();
			
			Asset next = null;
			
			@Override
			public boolean hasNext()
			{
				if(next != null)
					return true;
				//If next == null, try to set it
				while(base.hasNext())
				{
					AssetHierarchy entry = base.next();
					if(entry.getName().endsWith(extension))
					{
						next = entry.topInstance();
						break;
					}
				}
				//Did we suceed etc
				return next != null;
			}

			@Override
			public Asset next()
			{
				//Try loading
				if(next == null)
					hasNext();
				//Null out reference and return it
				Asset ret = next;
				next = null;
				return ret;
			}
			
		};
	}
	
	@Override
	public Iterator<Asset> getAllAssetsByPrefix(String prefix)
	{
		return new Iterator<Asset>() {

			Iterator<ModsAssetHierarchy> base = getAllUniqueEntries();
			
			Asset next = null;
			
			@Override
			public boolean hasNext()
			{
				if(next != null)
					return true;
				//If next == null, try to set it
				while(base.hasNext())
				{
					AssetHierarchy entry = base.next();
					if(entry.getName().startsWith(prefix))
					{
						next = entry.topInstance();
						break;
					}
				}
				//Did we suceed etc
				return next != null;
			}

			@Override
			public Asset next()
			{
				//Try loading
				if(next == null)
					hasNext();
				//Null out reference and return it
				Asset ret = next;
				next = null;
				return ret;
			}
			
		};
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getClassByName(java.lang.String)
	 */
	@Override
	public Class<?> getClassByName(String className)
	{
		//First try to load it from classpath
		try
		{
			Class<?> inBaseClasspath = Class.forName(className);
			if(inBaseClasspath != null)
				return inBaseClasspath;
		}
		catch (ClassNotFoundException e)
		{
			//We don't really care about this
			//e.printStackTrace();
		}
		//If this fails, try to obtain it from one of the loaded mods
		ChunkStoriesLogger.getInstance().log("Looking for class "+className+" in loaded mods", LogLevel.DEBUG);
		
		ForeignCodeClassLoader loader = avaibleForeignClasses.get(className);
		
		if(loader == null)
		{
			ChunkStoriesLogger.getInstance().log("Class "+className + " was not found in any loaded mod.", LogLevel.ERROR);
			return null;
		}
		
		Class<?> loadedClass = loader.obtainClass(className);
		
		if(loadedClass != null)
			return loadedClass;
		
		ChunkStoriesLogger.getInstance().log("WARNING: Failed to load class "+className, LogLevel.ERROR);
		
		//If all fail, return null
		return null;
	}	
	
	public class ModsAssetHierarchy implements AssetHierarchy
	{
		String assetName;
		Asset topInstance;
		Deque<Asset> instances;

		ModsAssetHierarchy(Asset asset)
		{
			assetName = asset.getName();
			instances = new ArrayDeque<Asset>();
			addAssetInstance(asset);

			//Lower complexity for just the top intance
			topInstance = asset;
		}

		@Override
		public String getName()
		{
			return assetName;
		}
		
		@Override
		public Asset topInstance()
		{
			return topInstance;
		}

		public void addAssetInstance(Asset asset)
		{
			instances.addLast(asset);
		}

		@Override
		public Iterator<Asset> iterator()
		{
			return instances.iterator();
		}

		//Below is hacks for HashSet to function properly
		public int hashCode()
		{
			return assetName.hashCode();
		}

		public boolean equals(Object o)
		{
			if (o instanceof String)
				return o.equals(assetName);
			if (o instanceof ModsAssetHierarchy)
				return ((ModsAssetHierarchy) o).assetName.equals(assetName);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getEnabledModsString()
	 */
	@Override
	public String[] getEnabledModsString()
	{
		return modsEnabled;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getCurrentlyLoadedMods()
	 */
	@Override
	public Collection<Mod> getCurrentlyLoadedMods()
	{
		return enabledMods;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.ModsManager#getModsPlugins()
	 */
	@Override
	public IterableIterator<PluginInformationImplementation> getAllModsPlugins()
	{
		return new IterableIterator<PluginInformationImplementation>(){

			Iterator<PluginInformationImplementation> i = pluginsWithinEnabledMods.iterator();
			
			@Override
			public boolean hasNext()
			{
				return i.hasNext();
			}

			@Override
			public PluginInformationImplementation next()
			{
				return i.next();
			}

			@Override
			public Iterator<PluginInformationImplementation> iterator()
			{
				return i;
			}
			
		};
	}
	
}
