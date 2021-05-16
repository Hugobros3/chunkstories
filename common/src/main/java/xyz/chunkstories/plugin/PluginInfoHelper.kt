package xyz.chunkstories.plugin

import com.google.gson.Gson
import org.hjson.JsonValue
import xyz.chunkstories.api.exceptions.plugins.PluginLoadException
import xyz.chunkstories.api.plugin.Plugin
import xyz.chunkstories.api.plugin.PluginInformation
import xyz.chunkstories.api.world.GameInstance
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

fun PluginInformation.createInstance(gameInstance: GameInstance, classLoader: ClassLoader): Plugin = try {
    val entryPointClassUnchecked = Class.forName(entryPoint, true, classLoader)

    // Checks for class fitness as an entry point
    if (!Plugin::class.java.isAssignableFrom(entryPointClassUnchecked))
        throw object : PluginLoadException() {
            override val message: String?
                get() {
                    return "Entry point not implementing ChunkStoriesPlugin or a subtype :$entryPoint"
                }
        }

    val entryPointClass = entryPointClassUnchecked.asSubclass(Plugin::class.java)

    val types = arrayOf(PluginInformation::class.java, GameInstance::class.java)
    val entryPointConstructor = entryPointClass.getConstructor(*types)

    entryPointConstructor.newInstance(this, gameInstance)
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