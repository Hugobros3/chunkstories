package xyz.chunkstories.plugin

import com.google.gson.Gson
import org.hjson.JsonValue
import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.exceptions.plugins.PluginLoadException
import xyz.chunkstories.api.plugin.ChunkStoriesPlugin
import xyz.chunkstories.api.plugin.PluginInformation
import java.io.File
import java.util.jar.JarFile

fun loadPluginInfo(file: File): PluginInformation? {
    try {
        val jar = JarFile(file)

        val pluginInfoHJsonStream = jar.getInputStream(jar.getJarEntry("plugin.json") ?: jar.getJarEntry("plugin.hjson"))
        val pluginInfoHJsonReader = pluginInfoHJsonStream.reader()

        val json = JsonValue.readHjson(pluginInfoHJsonReader).toString()

        val gson = Gson()

        val pluginInfo = gson.fromJson(json, PluginInformation::class.java)

        jar.close()

        return pluginInfo
    } catch (e: Exception) {
        println(e)
        e.printStackTrace()
    }
    return null
}

fun PluginInformation.createInstance(gameContext: GameContext, classLoader: ClassLoader): ChunkStoriesPlugin = try {
    val entryPointClassUnchecked = Class.forName(entryPoint, true, classLoader)

    // Checks for class fitness as an entry point
    if (!ChunkStoriesPlugin::class.java.isAssignableFrom(entryPointClassUnchecked))
        throw object : PluginLoadException() {
            override val message: String?
                get() {
                    return "Entry point not implementing ChunkStoriesPlugin or a subtype :$entryPoint"
                }
        }

    val entryPointClass = entryPointClassUnchecked.asSubclass(ChunkStoriesPlugin::class.java)

    val types = arrayOf(PluginInformation::class.java, GameContext::class.java)
    val entryPointConstructor = entryPointClass.getConstructor(*types)

    entryPointConstructor.newInstance(this, gameContext)


} catch (e: ClassNotFoundException) {
    throw object : PluginLoadException() {
        override val message: String
            get(): String = "Entry point class not found :$entryPoint"
    }
} catch (e: NoSuchMethodException) {
    throw object : PluginLoadException() {
        override val message: String
            get() = ("Suitable constructor for plugin type $pluginType not found in class :$entryPoint")
    }
} catch (e: SecurityException) {
    throw object : PluginLoadException() {
        override val message: String
            get() = "Suitable constructor for plugin type $pluginType not found in class :$entryPoint"
    }
}