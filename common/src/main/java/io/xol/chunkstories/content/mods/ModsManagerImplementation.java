//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content.mods;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.AssetHierarchy;
import io.xol.chunkstories.api.content.mods.Mod;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.api.exceptions.content.mods.ModLoadFailureException;
import io.xol.chunkstories.api.exceptions.content.mods.ModNotFoundException;
import io.xol.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException;
import io.xol.chunkstories.api.exceptions.plugins.PluginLoadException;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.sandbox.ForeignCodeClassLoader;
import io.xol.chunkstories.plugin.NotAPluginException;
import io.xol.chunkstories.plugin.PluginInformationImplementation;
import io.xol.chunkstories.util.FoldersUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class ModsManagerImplementation implements ModsManager {
    private final File baseContentLocation;

    private String[] modsToEnableInOrder = new String[0];

    private ModImplementation baseContent;

    private Map<String, Mod> mods = new HashMap<>();
    private List<Mod> modsInOrder = new LinkedList<>();
    private Map<String, ModsAssetHierarchy> avaibleAssets = new HashMap<>();
    private Map<String, ForeignCodeClassLoader> avaibleForeignClasses = new HashMap<>();

    private File cacheFolder = null;

    //TODO move, probably
    private List<PluginInformationImplementation> pluginsWithinEnabledMods = new ArrayList<>();

    private ClassLoader finalClassLoader = null;

    public ModsManagerImplementation(File coreContentLocation) throws NonExistentCoreContent {
        this(coreContentLocation, null);
    }

    public ModsManagerImplementation(File coreContentLocation, String enabledModsAtStart)
            throws NonExistentCoreContent {
        if (!coreContentLocation.exists())
            throw new NonExistentCoreContent();

        this.baseContentLocation = coreContentLocation;

        if (enabledModsAtStart != null)
            modsToEnableInOrder = enabledModsAtStart.split(",");
    }

    public class NonExistentCoreContent extends Exception {
        private static final long serialVersionUID = 8127068941907724484L;
    }

    @Override
    public void setEnabledMods(String... modsEnabled) {
        this.modsToEnableInOrder = modsEnabled;
    }

    @Override
    public void loadEnabledMods() throws NotAllModsLoadedException {
        mods.clear();
        modsInOrder.clear();

        List<ModLoadFailureException> modLoadExceptions = findAndLoadMods();

        buildModsFileSystem();

        // Return an exception if some mods failed to load.
        if (modLoadExceptions.size() > 0)
            throw new NotAllModsLoadedException(modLoadExceptions);
    }

    /**
     * Looks for the mods the user asked for and loads their base information.
     * Returns a list of exceptions for any mod that failed to load
     */
    private List<ModLoadFailureException> findAndLoadMods() {
        List<ModLoadFailureException> modLoadExceptions = new LinkedList<>();

        // Creates mods dir if it needs to
        File modsDir = new File(GameDirectory.getGameFolderPath() + "/mods");
        if (!modsDir.exists())
            modsDir.mkdirs();

        // Server mods
        File serverMods = new File(GameDirectory.getGameFolderPath() + "/servermods");
        if (!serverMods.exists())
            serverMods.mkdirs();

        for (String name : modsToEnableInOrder) {
            try {
                ModImplementation mod = null;

                // Servers give a md5 hash for their required mods
                if (name.startsWith("md5:")) {
                    // Look for a mod with that md5 hash
                    String hash = name.substring(4, name.length());
                    String url = null;
                    // If the hash is bundled with an url, split'em
                    if (hash.contains(":")) {
                        int i = hash.indexOf(":");
                        url = hash.substring(i + 1);
                        hash = hash.substring(0, i);
                    }
                    logger.debug("Looking for hashed mod " + hash + " (url = " + url + ")");

                    // Look for the mod zip in local fs first.
                    File zippedMod = new File(serverMods.getAbsolutePath() + "/" + hash + ".zip");
                    if (zippedMod.exists()) {
                        // Awesome we found it !
                        mod = new ModZip(zippedMod);
                    } else if (url != null) {
                        // TODO download and handle files from server
                    } else {
                        // We failed. Mod won't be loaded
                    }
                } else {
                    logger.debug("Looking for mod " + name + " on the local filesystem");

                    // First look for it in the directory section
                    File modDirectory = new File(modsDir.getAbsolutePath() + "/" + name);
                    if (modDirectory.exists()) {
                        mod = new ModFolder(modDirectory);
                        logger.debug("Found mod in directory : " + modDirectory);
                    } else {
                        // Then look for a .zip file in the same directory
                        File zippedMod = new File(modsDir.getAbsolutePath() + "/" + name + ".zip");
                        if (zippedMod.exists()) {
                            mod = new ModZip(zippedMod);
                            logger.debug("Found mod in zipfile : " + zippedMod);
                        } else {
                            // Finally just look for it in the global os path
                            if (name.endsWith(".zip")) {
                                zippedMod = new File(name);
                                if (zippedMod.exists()) {
                                    mod = new ModZip(zippedMod);
                                    logger.debug("Found mod in global zipfile : " + zippedMod);
                                }
                            } else {
                                modDirectory = new File(name);
                                if (modDirectory.exists()) {
                                    mod = new ModFolder(modDirectory);
                                    logger.debug("Found mod in global directory : " + modDirectory);
                                }
                            }
                        }
                    }
                }

                // Did we manage it ?
                if (mod == null)
                    throw new ModNotFoundException(name);

                if (mods.put(mod.getModInfo().getInternalName(), mod) != null)
                    throw new ModLoadFailureException(mod, "Conflicting mod, another mod with the same name or hash is already loaded.");
                modsInOrder.add(mod);

            } catch (ModLoadFailureException exception) {
                modLoadExceptions.add(exception);
            }
        }

        return modLoadExceptions;
    }

    /** (Re) builds the game virtual filesystem */
    private void buildModsFileSystem() {
        avaibleAssets.clear();
        avaibleForeignClasses.clear();

        pluginsWithinEnabledMods.clear();

        // Obtain a cache folder
        if (cacheFolder == null) {
            cacheFolder = new File(GameDirectory.getGameFolderPath() + "/cache/" + ((int) (Math.random() * 10000)));
            cacheFolder.mkdirs();
            // cacheFolder.deleteOnExit();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Deleting cache folder " + cacheFolder);
                FoldersUtils.deleteFolder(cacheFolder);
            }));
        }

        // Checks for the base assets folder presence and sanity
        try {
            if (baseContentLocation.isDirectory())
                baseContent = new ModFolder(baseContentLocation);
            else
                baseContent = new ModZip(baseContentLocation);
        } catch (ModLoadFailureException e) {
            logger().error("Fatal : failed to load in the base assets folder. Exception : {}", e);
        }

        ClassLoader childClassLoader = loadModAssets(baseContent, Thread.currentThread().getContextClassLoader());
        // Iterates over mods, in order of priority (lowest to highest)
        for (Mod mod : modsInOrder) {
            childClassLoader = loadModAssets((ModImplementation) mod, childClassLoader);
        }

        finalClassLoader = childClassLoader;
    }

    private ClassLoader loadModAssets(ModImplementation mod, ClassLoader parentClassLoader) {
        List<File> jarFiles = new LinkedList<>();

        // For each asset in the said mod
        for (Asset asset : mod.assets()) {
            // Skips mod.txt
            if (asset.getName().equals("mod.txt"))
                continue;

            // Special case for .jar files : we extract them in the cache/ folder and make
            // them avaible through secure ClassLoaders
            if (asset.getName().endsWith(".jar")) {
                File jarFile = loadJarFile(asset);
                if (jarFile != null)
                    jarFiles.add(jarFile);
                continue;
            }

            // Look for it's entry
            ModsAssetHierarchy entry = avaibleAssets.get(asset.getName());
            if (entry == null) {
                entry = new ModsAssetHierarchy(asset);
                avaibleAssets.put(asset.getName(), entry);
            } else {
                logger.debug("Adding asset " + asset + ", overriding previous stuff (top=" + entry.topInstance() + ")");
                entry.addAssetInstance(asset);
            }
        }

        // No jar files ? Just return the parent class loader.
        if (jarFiles.size() == 0)
            return parentClassLoader;

        // Build a specialized class loader based on the parent one and the jars found
        // within the mod
        ForeignCodeClassLoader classLoader;
        try {
            classLoader = new ForeignCodeClassLoader(mod, parentClassLoader, jarFiles);
        } catch (IOException e1) {
            e1.printStackTrace();
            logger.error("Error whilst creating a ForeignCodeClassLoader for " + mod);
            return parentClassLoader;
        }

        for (String className : classLoader.classes()) {
            avaibleForeignClasses.put(className, classLoader);
        }

        // Load any plugins those jar files might be
        for (File jarFile : jarFiles) {
            try {
                PluginInformationImplementation pluginInformation = new PluginInformationImplementation(jarFile,
                        classLoader);
                logger.info("Found plugin " + pluginInformation + " in " + mod);
                pluginsWithinEnabledMods.add(pluginInformation);
            } catch (NotAPluginException nap) {
                // Discard silently
            } catch (PluginLoadException | IOException e) {
                System.out.println("Something went wrong loading the plugin " + jarFile + " from " + mod);
                e.printStackTrace();
            }
        }

        return classLoader;
    }

    private File loadJarFile(Asset asset) {
        logger.info("Handling jar file " + asset);
        try {
            // Read the jar file contents and extract it somewhere on cache
            // TODO hash dat crap this boi, the collision probs!!!
            int random = ((int) (Math.random() * 16384960));
            File cachedJarLocation = new File(cacheFolder.getAbsolutePath() + "/" + random + ".jar");
            FileOutputStream fos = new FileOutputStream(cachedJarLocation);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            InputStream is = asset.read();
            logger.debug("Writing to " + cachedJarLocation);
            byte[] buf = new byte[4096];
            while (is.available() > 0) {
                int read = is.read(buf);
                bos.write(buf, 0, read);
                if (read == 0)
                    break;
            }
            bos.flush();
            bos.close();
            logger.debug("Done writing file");

            return cachedJarLocation;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Iterator<AssetHierarchy> getAllUniqueEntries() {
        return new Iterator<AssetHierarchy>() {

            Iterator<ModsAssetHierarchy> i = avaibleAssets.values().iterator();

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public AssetHierarchy next() {
                return i.next();
            }

        };
    }

    @Override
    public Iterator<Asset> getAllUniqueFilesLocations() {
        return new Iterator<Asset>() {
            Iterator<ModsAssetHierarchy> i = avaibleAssets.values().iterator();

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public Asset next() {
                return i.next().topInstance();
            }

        };
    }

    @Override
    public Asset getAsset(String assetName) {
        assert assetName != null;
        ModsAssetHierarchy asset = avaibleAssets.get(assetName);

        if (asset == null && assetName.startsWith("./")) {
            logger.warn("Requesting asset using the old deprecated ./ prefix !");
            Thread.dumpStack();
            asset = avaibleAssets.get(assetName.substring(2));
        }

        if (assetName.startsWith("@")) {
            String[] strippedAndSplit = assetName.substring(1).split(":");
            String modName = strippedAndSplit[0];
            assetName = strippedAndSplit[1];

            Mod mod = mods.get(modName);
        }

        if (asset == null)
            return null;

        return asset.topInstance();
    }

    @Override
    public ModsAssetHierarchy getAssetInstances(String assetName) {
        return avaibleAssets.get(assetName);
    }

    @Override
    public Iterator<Asset> getAllAssetsByExtension(String extension) {
        return new Iterator<Asset>() {

            Iterator<ModsAssetHierarchy> base = avaibleAssets.values().iterator();

            Asset next = null;

            @Override
            public boolean hasNext() {
                if (next != null)
                    return true;
                // If next == null, try to set it
                while (base.hasNext()) {
                    AssetHierarchy entry = base.next();
                    if (entry.getName().endsWith(extension)) {
                        next = entry.topInstance();
                        break;
                    }
                }
                // Did we suceed etc
                return next != null;
            }

            @Override
            public Asset next() {
                // Try loading
                if (next == null)
                    hasNext();
                // Null out reference and return it
                Asset ret = next;
                next = null;
                return ret;
            }

        };
    }

    @Override
    public Iterator<Asset> getAllAssetsByPrefix(String prefix) {
        return new Iterator<Asset>() {

            Iterator<ModsAssetHierarchy> base = avaibleAssets.values().iterator();

            Asset next = null;

            @Override
            public boolean hasNext() {
                if (next != null)
                    return true;
                // If next == null, try to set it
                while (base.hasNext()) {
                    AssetHierarchy entry = base.next();
                    if (entry.getName().startsWith(prefix)) {
                        next = entry.topInstance();
                        break;
                    }
                }
                // Did we suceed etc
                return next != null;
            }

            @Override
            public Asset next() {
                // Try loading
                if (next == null)
                    hasNext();
                // Null out reference and return it
                Asset ret = next;
                next = null;
                return ret;
            }

        };
    }

    @Override
    public Class<?> getClassByName(String className) {
        // TODO: Make this absolutely unecessary
        try {
            Class<?> inBaseClasspath = Class.forName(className);
            if (inBaseClasspath != null)
                return inBaseClasspath;
        } catch (ClassNotFoundException e) {
            // We don't really care about this
            // e.printStackTrace();
        }

        ForeignCodeClassLoader loader = avaibleForeignClasses.get(className);

        if (loader == null) {
            logger().warn("Class " + className + " was not found in any loaded mod.");
            return null;
        }

        Class<?> loadedClass = loader.obtainClass(className);

        if (loadedClass != null)
            return loadedClass;

        logger().error("Failed to load class " + className);

        // If all fail, return null
        return null;
    }

    public class ModsAssetHierarchy implements AssetHierarchy {
        String assetName;
        Asset topInstance;
        Deque<Asset> instances;

        ModsAssetHierarchy(Asset asset) {
            assetName = asset.getName();
            instances = new ArrayDeque<>();
            addAssetInstance(asset);

            // Lower complexity for just the top intance
            topInstance = asset;
        }

        @Override
        public String getName() {
            return assetName;
        }

        @Override
        public Asset topInstance() {
            return topInstance;
        }

        public void addAssetInstance(Asset asset) {
            instances.addFirst(asset);
            topInstance = asset;
        }

        @Override
        public Iterator<Asset> iterator() {
            return instances.iterator();
        }

        // Below is hacks for HashSet to function properly
        public int hashCode() {
            return assetName.hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof String)
                return o.equals(assetName);
            if (o instanceof ModsAssetHierarchy)
                return ((ModsAssetHierarchy) o).assetName.equals(assetName);
            return false;
        }
    }

    @Override
    public String[] getEnabledModsString() {
        return modsToEnableInOrder;
    }

    @Override
    public Collection<Mod> getCurrentlyLoadedMods() {
        return modsInOrder;
    }

    public IterableIterator<PluginInformationImplementation> getAllModsPlugins() {
        return new IterableIterator<PluginInformationImplementation>() {

            Iterator<PluginInformationImplementation> i = pluginsWithinEnabledMods.iterator();

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public PluginInformationImplementation next() {
                return i.next();
            }

            @Override
            public Iterator<PluginInformationImplementation> iterator() {
                return i;
            }

        };
    }

    private static final Logger logger = LoggerFactory.getLogger("content.modsManager");

    public Logger logger() {
        return logger;
    }

    public ClassLoader getFinalClassLoader() {
        return finalClassLoader;
    }

}
