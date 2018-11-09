//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content.mods

import io.xol.chunkstories.api.content.Asset
import io.xol.chunkstories.api.content.mods.AssetHierarchy
import io.xol.chunkstories.api.content.mods.Mod
import io.xol.chunkstories.api.content.mods.ModsManager
import io.xol.chunkstories.api.exceptions.content.mods.ModLoadFailureException
import io.xol.chunkstories.api.exceptions.content.mods.ModNotFoundException
import io.xol.chunkstories.api.exceptions.content.mods.NotAllModsLoadedException
import io.xol.chunkstories.api.exceptions.plugins.PluginLoadException
import io.xol.chunkstories.content.GameDirectory
import io.xol.chunkstories.content.sandbox.ForeignCodeClassLoader
import io.xol.chunkstories.plugin.NotAPluginException
import io.xol.chunkstories.plugin.PluginInformationImplementation
import io.xol.chunkstories.util.FoldersUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.*
import java.util.*

class ModsManagerImplementation @Throws(NonExistentCoreContent::class)
@JvmOverloads constructor(private val baseContentLocation: File, enabledModsAtStart: String? = null) : ModsManager {

    private var modsToEnableInOrder: Array<String> = emptyArray()

    private lateinit var baseContent: ModImplementation

    private val mods = HashMap<String, Mod>()
    private val modsInOrder = LinkedList<Mod>()

    private lateinit var allEntriesCached: Collection<AssetHierarchy>
    private lateinit var allAssetsCached: Collection<Asset>

    private val avaibleAssets = HashMap<String, ModsAssetHierarchy>()
    private val avaibleForeignClasses = HashMap<String, ForeignCodeClassLoader>()

    private var cacheFolder: File? = null

    //TODO move, probably
    private val pluginsWithinEnabledMods = ArrayList<PluginInformationImplementation>()

    var finalClassLoader: ClassLoader? = null
        private set

    lateinit var allModsPlugins: Collection<PluginInformationImplementation>
        private set

    init {
        if (!baseContentLocation.exists())
            throw NonExistentCoreContent()

        if (enabledModsAtStart != null)
            modsToEnableInOrder = enabledModsAtStart.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    inner class NonExistentCoreContent : Exception() {
        /*companion object {
            private val serialVersionUID = 8127068941907724484L
        }*/
    }

    override fun setEnabledMods(vararg modsEnabled: String) {
        this.modsToEnableInOrder = modsEnabled as Array<String>
    }

    @Throws(NotAllModsLoadedException::class)
    override fun loadEnabledMods() {
        mods.clear()
        modsInOrder.clear()

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
        val modsDir = File(GameDirectory.getGameFolderPath() + "/mods")
        if (!modsDir.exists())
            modsDir.mkdirs()

        // Server mods
        val serverMods = File(GameDirectory.getGameFolderPath() + "/servermods")
        if (!serverMods.exists())
            serverMods.mkdirs()

        for (name in modsToEnableInOrder) {
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

                if (mods.put(mod.getModInfo().internalName, mod) != null)
                    throw ModLoadFailureException(mod, "Conflicting mod, another mod with the same name or hash is already loaded.")
                modsInOrder.add(mod)

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
            cacheFolder = File(GameDirectory.getGameFolderPath() + "/cache/" + (Math.random() * 10000).toInt())
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
        for (mod in modsInOrder) {
            childClassLoader = loadModAssets(mod as ModImplementation, childClassLoader)
        }

        //Cache all the assets into immutable collections
        allEntriesCached = Collections.unmodifiableCollection(this.avaibleAssets.map { it.value as AssetHierarchy })
        allAssetsCached = Collections.unmodifiableCollection(allUniqueEntries.map { it.topInstance })

        finalClassLoader = childClassLoader
    }

    private fun loadModAssets(mod: ModImplementation, parentClassLoader: ClassLoader): ClassLoader {
        val jarFiles = LinkedList<File>()

        // For each asset in the said mod
        for (asset in mod.assets()) {
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
                val pluginInformation = PluginInformationImplementation(jarFile,
                        classLoader)
                logger.info("Found plugin $pluginInformation in $mod")
                pluginsWithinEnabledMods.add(pluginInformation)
            } catch (nap: NotAPluginException) {
                // Discard silently
            } catch (e: PluginLoadException) {
                println("Something went wrong loading the plugin $jarFile from $mod")
                e.printStackTrace()
            } catch (e: IOException) {
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

    override fun getAllUniqueEntries(): Collection<AssetHierarchy> = allEntriesCached

    override fun getAllAssets(): Collection<Asset> = allAssetsCached

    override fun getAsset(assetName: String?): Asset? {
        var assetName = assetName!!
        var asset: ModsAssetHierarchy? = avaibleAssets[assetName]

        if (asset == null && assetName.startsWith("./")) {
            logger.warn("Requesting asset using the old deprecated ./ prefix !")
            Thread.dumpStack()
            asset = avaibleAssets[assetName.substring(2)]
        }

        if (assetName.startsWith("@")) {
            val strippedAndSplit = assetName.substring(1).split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val modName = strippedAndSplit[0]
            assetName = strippedAndSplit[1]

            val mod = mods[modName]
        }

        return asset?.topInstance

    }

    override fun getAssetInstances(assetName: String): ModsAssetHierarchy? {
        return avaibleAssets[assetName]
    }

    override fun getAllAssetsByExtension(extension: String): Collection<Asset> = allAssetsCached.filter { it.name.endsWith(extension) }
    override fun getAllAssetsByPrefix(prefix: String): Collection<Asset> = allAssetsCached.filter { it.name.startsWith(prefix) }

    override fun getClassByName(className: String): Class<*>? {
        // TODO: Make this absolutely unecessary
        try {
            val inBaseClasspath = Class.forName(className)
            if (inBaseClasspath != null)
                return inBaseClasspath
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

    override fun getEnabledModsString(): Array<String> {
        return modsToEnableInOrder
    }

    override fun getCurrentlyLoadedMods(): Collection<Mod> {
        return modsInOrder
    }

    fun logger(): Logger {
        return logger
    }

    companion object {

        private val logger = LoggerFactory.getLogger("content.modsManager")
    }

}
