//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content.mods

import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.mods.AssetHierarchy
import xyz.chunkstories.api.content.mods.Mod
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.exceptions.content.mods.ModLoadFailureException
import xyz.chunkstories.api.exceptions.content.mods.ModNotFoundException
import xyz.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException
import xyz.chunkstories.content.sandbox.ForeignCodeClassLoader
import xyz.chunkstories.util.FoldersUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.plugin.PluginInformation
import xyz.chunkstories.plugin.loadPluginInfo

import java.io.*
import java.net.URLClassLoader
import java.util.*

class ModsManagerImplementation
constructor(private val baseContentLocation: File, var requestedMods: List<String>) : ModsManager {
    private lateinit var baseContent: ModImplementation

    private val mods = HashMap<String, Mod>()
    private val loadedMods = mutableListOf<Mod>()

    private lateinit var allEntriesCached: Collection<AssetHierarchy>
    private lateinit var allAssetsCached: Collection<Asset>

    private val avaibleAssets = HashMap<String, ModsAssetHierarchy>()
    private val avaibleForeignClasses = HashMap<String, ForeignCodeClassLoader>()

    private var cacheFolder: File? = null

    //TODO move, probably
    val pluginsWithinEnabledMods = ArrayList<PluginInformation>()

    var finalClassLoader: ClassLoader? = null
        private set

    init {
        if (!baseContentLocation.exists())
            throw Exception("Missing core content: file $baseContentLocation does not exist")
    }

    @Throws(NotAllModsLoadedException::class)
    fun loadEnabledMods() {
        mods.clear()
        loadedMods.clear()

        val modLoadExceptions = findAndLoadMods()
        buildModsFileSystem()

        // Return an exception if some mods failed to load.
        if (modLoadExceptions.isNotEmpty())
            throw NotAllModsLoadedException(modLoadExceptions)
    }

    /**
     * Looks for the mods the user asked for and loads their base information.
     * Returns a list of exceptions for any mod that failed to load
     */
    private fun findAndLoadMods(): List<ModLoadFailureException> {
        val modLoadExceptions = LinkedList<ModLoadFailureException>()

        // Creates mods dir if it needs to
        val modsDir = File("." + "/mods")
        if (!modsDir.exists())
            modsDir.mkdirs()

        // Server mods
        val serverMods = File("." + "/servermods")
        if (!serverMods.exists())
            serverMods.mkdirs()

        for (name in requestedMods) {
            try {
                var mod: ModImplementation? = null

                // Servers give a md5 hash for their required mods
                if (name.startsWith("md5:")) {
                    // Look for a mod with that md5 hash
                    var hash = name.substring(4, name.length)
                    var url: String? = null
                    // If the hash is bundled with an url, split'em
                    if (hash.contains(":")) {
                        val i = hash.indexOf(":")
                        url = hash.substring(i + 1)
                        hash = hash.substring(0, i)
                    }
                    logger.debug("Looking for hashed mod $hash (url = $url)")

                    // Look for the mod zip in local fs first.
                    val zippedMod = File(serverMods.absolutePath + "/" + hash + ".zip")
                    if (zippedMod.exists()) {
                        // Awesome we found it !
                        mod = ModZip(zippedMod)
                    } else if (url != null) {
                        // TODO download and handle files from server
                    } else {
                        // We failed. Mod won't be loaded
                    }
                } else {
                    logger.debug("Looking for mod $name on the local filesystem")

                    // First look for it in the directory section
                    var modDirectory = File(modsDir.absolutePath + "/" + name)
                    if (modDirectory.exists()) {
                        mod = ModFolder(modDirectory)
                        logger.debug("Found mod in directory : $modDirectory")
                    } else {
                        // Then look for a .zip file in the same directory
                        var zippedMod = File(modsDir.absolutePath + "/" + name + ".zip")
                        if (zippedMod.exists()) {
                            mod = ModZip(zippedMod)
                            logger.debug("Found mod in zipfile : $zippedMod")
                        } else {
                            // Finally just look for it in the global os path
                            if (name.endsWith(".zip")) {
                                zippedMod = File(name)
                                if (zippedMod.exists()) {
                                    mod = ModZip(zippedMod)
                                    logger.debug("Found mod in global zipfile : $zippedMod")
                                }
                            } else {
                                modDirectory = File(name)
                                if (modDirectory.exists()) {
                                    mod = ModFolder(modDirectory)
                                    logger.debug("Found mod in global directory : $modDirectory")
                                }
                            }
                        }
                    }
                }

                // Did we manage it ?
                if (mod == null)
                    throw ModNotFoundException(name)

                if (mods.put(mod.modInfo.internalName, mod) != null)
                    throw ModLoadFailureException(mod, "Conflicting mod, another mod with the same name or hash is already loaded.")
                loadedMods.add(mod)

            } catch (exception: ModLoadFailureException) {
                modLoadExceptions.add(exception)
            }

        }

        return modLoadExceptions
    }

    /** (Re) builds the game virtual filesystem  */
    private fun buildModsFileSystem() {
        avaibleAssets.clear()
        avaibleForeignClasses.clear()

        pluginsWithinEnabledMods.clear()

        // Obtain a cache folder
        if (cacheFolder == null) {
            cacheFolder = File("." + "/cache/" + (Math.random() * 10000).toInt())
            cacheFolder!!.mkdirs()
            // cacheFolder.deleteOnExit();
            Runtime.getRuntime().addShutdownHook(Thread {
                println("Deleting cache folder " + cacheFolder!!)
                FoldersUtils.deleteFolder(cacheFolder)
            })
        }

        // Checks for the base assets folder presence and sanity
        try {
            if (baseContentLocation.isDirectory)
                baseContent = ModFolder(baseContentLocation)
            else
                baseContent = ModZip(baseContentLocation)
        } catch (e: ModLoadFailureException) {
            logger().error("Fatal : failed to load in the base assets folder. Exception : {}", e)
        }

        var childClassLoader = loadModAssets(baseContent, Thread.currentThread().contextClassLoader)
        // Iterates over mods, in order of priority (lowest to highest)
        for (mod in loadedMods) {
            childClassLoader = loadModAssets(mod as ModImplementation, childClassLoader)
        }

        // Add the plugin classes stuff
        val pluginsFolder = File("./plugins/")
        pluginsFolder.mkdirs()
        val plugins = pluginsFolder.listFiles()?.filter { it.isFile && it.name.endsWith(".jar") } ?: emptyList()
        childClassLoader = URLClassLoader(plugins.map { it.toURL() }.toTypedArray(), childClassLoader)

        //Cache all the assets into immutable collections
        allEntriesCached = Collections.unmodifiableCollection(this.avaibleAssets.map { it.value as AssetHierarchy })
        allAssetsCached = Collections.unmodifiableCollection(allUniqueEntries.map { it.topInstance })

        finalClassLoader = childClassLoader
    }

    private fun loadModAssets(mod: ModImplementation, parentClassLoader: ClassLoader): ClassLoader {
        val jarFiles = LinkedList<File>()

        // For each asset in the said mod
        for (asset in mod.assets) {
            // Skips mod.txt
            if (asset.name == "mod.txt")
                continue

            // Special case for .jar files : we extract them in the cache/ folder and make
            // them avaible through secure ClassLoaders
            if (asset.name.endsWith(".jar")) {
                val jarFile = loadJarFile(asset)
                if (jarFile != null)
                    jarFiles.add(jarFile)
                continue
            }

            // Look for it's entry
            var entry: ModsAssetHierarchy? = avaibleAssets[asset.name]
            if (entry == null) {
                entry = ModsAssetHierarchy(asset)
                avaibleAssets[asset.name] = entry
            } else {
                logger.debug("Adding asset " + asset + ", overriding previous stuff (top=" + entry.topInstance + ")")
                entry.addAssetInstance(asset)
            }
        }

        // No jar files ? Just return the parent class loader.
        if (jarFiles.size == 0)
            return parentClassLoader

        // Build a specialized class loader based on the parent one and the jars found
        // within the mod
        val classLoader: ForeignCodeClassLoader
        try {
            classLoader = ForeignCodeClassLoader(mod, parentClassLoader, jarFiles)
        } catch (e1: IOException) {
            e1.printStackTrace()
            logger.error("Error whilst creating a ForeignCodeClassLoader for $mod")
            return parentClassLoader
        }

        for (className in classLoader.classes()) {
            avaibleForeignClasses[className] = classLoader
        }

        // Load any plugins those jar files might be
        for (jarFile in jarFiles) {
            try {
                val pluginInformation = loadPluginInfo(jarFile) ?: continue
                logger.info("Found plugin $pluginInformation in $mod")
                pluginsWithinEnabledMods.add(pluginInformation)
            } catch (e: Exception) {
                println("Something went wrong loading the plugin $jarFile from $mod")
                e.printStackTrace()
            }
        }

        return classLoader
    }

    private fun loadJarFile(asset: Asset): File? {
        logger.info("Handling jar file $asset")
        try {
            // Read the jar file contents and extract it somewhere on cache
            // TODO hash dat crap this boi, the collision probs!!!
            val random = (Math.random() * 16384960).toInt()
            val cachedJarLocation = File(cacheFolder!!.absolutePath + "/" + random + ".jar")
            val fos = FileOutputStream(cachedJarLocation)
            val bos = BufferedOutputStream(fos)
            val `is` = asset.read()
            logger.debug("Writing to $cachedJarLocation")
            val buf = ByteArray(4096)
            while (`is`.available() > 0) {
                val read = `is`.read(buf)
                bos.write(buf, 0, read)
                if (read == 0)
                    break
            }
            bos.flush()
            bos.close()
            logger.debug("Done writing file")

            return cachedJarLocation
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    val allUniqueEntries: Collection<AssetHierarchy>
        get() = allEntriesCached

    override val allAssets: Collection<Asset>
        get() = allAssetsCached

    override fun getAsset(assetName: String): Asset? {
        var filteredAssetName = assetName

        if (filteredAssetName.startsWith("./")) {
            logger.warn("Requesting asset using the old deprecated ./ prefix !")
            //Thread.dumpStack()
            filteredAssetName = filteredAssetName.substring(2)
        }

        if(filteredAssetName.startsWith("@")) {
            val withoutPrefix = filteredAssetName.removePrefix("@")
            val i = filteredAssetName.indexOf(':')
            if(i < 0)
                throw Exception("Malformed asset name: $filteredAssetName")

            val modInternalName = withoutPrefix.subSequence(0, i - 1)
            filteredAssetName = withoutPrefix.substring(i)

            return mods[modInternalName]?.getAssetByName(filteredAssetName)
        }

        val asset: ModsAssetHierarchy? = avaibleAssets[filteredAssetName]

        return asset?.topInstance

    }

    override fun getAssetInstances(assetName: String): ModsAssetHierarchy? {
        return avaibleAssets[assetName]
    }

    fun getAllAssetsByExtension(extension: String): Collection<Asset> = allAssetsCached.filter { it.name.endsWith(extension) }
    override fun getAllAssetsByPrefix(prefix: String): Collection<Asset> = allAssetsCached.filter { it.name.startsWith(prefix) }

    override fun getClassByName(className: String): Class<*>? {
        // TODO: Make this absolutely unnecessary
        try {
            return Class.forName(className)
        } catch (ignore: ClassNotFoundException) {
        }

        val loader = avaibleForeignClasses[className]

        if (loader == null) {
            logger().warn("Class $className was not found in any loaded mod.")
            return null
        }

        val loadedClass = loader.obtainClass(className)

        if (loadedClass != null)
            return loadedClass

        logger().error("Failed to load class $className")

        // If all fail, return null
        return null
    }

    inner class ModsAssetHierarchy internal constructor(override var topInstance: Asset) : AssetHierarchy {
        override val name: String = topInstance.name
        override val instances: MutableList<Asset> = mutableListOf()

        init {
            addAssetInstance(topInstance)
        }

        fun addAssetInstance(asset: Asset) {
            instances.add(0, asset)
            topInstance = asset
        }

        // Below is hacks for HashSet to function properly
        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun equals(o: Any?): Boolean {
            if (o is String)
                return o == name
            return if (o is ModsAssetHierarchy) o.name == name else false
        }
    }

    override val currentlyLoadedMods: Collection<Mod>
        get() = loadedMods

    fun logger(): Logger {
        return logger
    }

    companion object {

        private val logger = LoggerFactory.getLogger("content.modsManager")
    }

}
