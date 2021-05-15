//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.plugin

import java.io.File
import java.io.IOException
import java.util.HashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.events.Event
import xyz.chunkstories.api.exceptions.plugins.PluginCreationException
import xyz.chunkstories.api.exceptions.plugins.PluginLoadException
import xyz.chunkstories.api.plugin.Plugin
import xyz.chunkstories.api.plugin.PluginInformation
import xyz.chunkstories.api.plugin.PluginInformation.PluginType
import xyz.chunkstories.api.plugin.PluginManager
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.plugin.commands.CommandHandler
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.world.GameInstance
import xyz.chunkstories.content.mods.ModsManagerImplementation

open class DefaultPluginManager(private val gameInstance: GameInstance) : PluginManager {
    override var activePlugins = mutableListOf<Plugin>()
    //private val registeredEventListeners = HashMap<EventListeners, RegisteredListener>()

    var commandsAliases: MutableMap<String, Command> = HashMap()
    override var commands = mutableListOf<Command>()

    override fun reloadPlugins() {
        // First disable any leftover plugins
        disablePlugins()

        // We make a list
        val pluginsToLoad = mutableListOf<PluginInformation>()

        // Creates plugins folder if it isn't present.
        val pluginsFolder = File("." + "/plugins/")
        pluginsFolder.mkdirs()

        // Iterates over files of the folder
        for (file in pluginsFolder.listFiles()!!) {
            if (file.name.endsWith(".jar")) {
                try {
                    val pluginInformation: PluginInformation = loadPluginInfo(file) ?: continue

                    // Checks type is appropriate
                    if (pluginInformation.pluginType == PluginType.CLIENT_ONLY && gameInstance !is Client ||
                            pluginInformation.pluginType == PluginType.MASTER && gameInstance !is Host)
                        continue

                    pluginsToLoad.add(pluginInformation)
                } catch (e: PluginLoadException) {
                    logger().error("Failed to load plugin file " + file + e.message)
                    e.printStackTrace()
                } catch (e: IOException) {
                    logger().error("Failed to load plugin file " + file + e.message)
                    e.printStackTrace()
                }

            }
        }

        // Mods too can bundle plugins
        for (pluginInformation in (this.gameInstance.content.modsManager as ModsManagerImplementation).pluginsWithinEnabledMods) {

            // Checks type is appropriate
            if (pluginInformation.pluginType == PluginType.CLIENT_ONLY && gameInstance !is Client ||
                    pluginInformation.pluginType == PluginType.MASTER && gameInstance !is Host)
                continue

            pluginsToLoad.add(pluginInformation)
        }

        // Enables the found plugins
        enablePlugins(pluginsToLoad)
    }

    private fun enablePlugins(pluginsToInitialize: List<PluginInformation>) {
        logger().info(pluginsToInitialize.size.toString() + " plugins to initialize")
        val finalClassLoader = (gameInstance.content.modsManager as ModsManagerImplementation).finalClassLoader!!
        // TODO sort plugins requirements (requires/before)
        for (pluginInformation in pluginsToInitialize) {
            try {
                activePlugins.add(pluginInformation.createInstance(gameInstance, finalClassLoader))
            } catch (e: PluginCreationException) {
                logger().error("Couldn't create plugin " + pluginInformation + " : " + e.message)
                e.printStackTrace()
            }

        }
    }

    fun disablePlugins() {
        // Call onDisable for plugins
        for (plugin in activePlugins)
            plugin.onDisable()

        // Remove one by one each listener
        // TODO
        /*for ((key, value) in registeredEventListeners) {
            key.unRegisterListener(value)
        }*/

        // Remove registered commands
        // TODO only remove plugins commands
        commandsAliases.clear()
        commands.clear()

        // At last clear the plugins list
        activePlugins.clear()
    }

    override fun registerCommand(commandName: String, commandHandler: CommandHandler, vararg aliases: String): Command {
        val command = Command(commandName)
        command.handler = commandHandler

        for (alias in aliases) {
            command.addAlias(alias)
            commandsAliases[alias] = command
        }

        this.commands.add(command)
        commandsAliases[commandName] = command

        return command
    }

    fun unregisterCommand(command: Command) {
        commands.remove(command)
        //TODO remove aliases
    }

    override fun dispatchCommand(emitter: CommandEmitter, commandName: String, arguments: Array<String>): Boolean {
        val command = findCommandUsingAlias(commandName) ?: return false

        try {
            val handler = command.handler

            if (handler != null)
                return handler.handleCommand(emitter, command, arguments)
            else
                emitter.sendMessage("#FF2020No handler defined for this command !")

            return false
        } catch (t: Throwable) {
            emitter.sendMessage("#FF4040 An exception was throwed when handling your command " + t.message)
            t.printStackTrace()
        }

        return false
    }

    override fun findCommandUsingAlias(commandName: String): Command? {
        return commandsAliases[commandName.toLowerCase()]
    }

    override fun registerEventListener(listener: Any, plugin: Plugin) {
    TODO()
    /*try {
            val methods = HashSet<Method>()
            for (method in listener.javaClass.methods)
                methods.add(method)
            // Filter it so only interested in @EventHandler annoted methods
            for (method in methods) {
                val eventHandlerAnnotation = method.getAnnotation(EventHandler::class.java) ?: continue

                // We look for the annotation in the method
                // TODO something about priority
                if (method.parameterTypes.size != 1 || !Event::class.java.isAssignableFrom(method.parameterTypes[0])) {
                    logger().warn("Plugin $plugin attempted to register an invalid EventHandler")
                    continue
                }

                val eventClassToListenTo = method.parameterTypes[0].asSubclass(Event::class.java)

                // Create an EventExecutor to launch the event code
                val executor = object: EventExecutor {
                    override fun fireEvent(event: Event) {
                        method.invoke(listener, event)
                    }
                }
                val registeredListener = RegisteredListener(listener, plugin, executor, eventHandlerAnnotation.priority)

                // Get the listeners list for this event

                val thisEventKindOfListeners = try {
                    val getListeners = eventClassToListenTo.getMethod("getListenersStatic")
                    getListeners.isAccessible = true
                    getListeners.invoke(null) as EventListeners
                } catch(e: NoSuchMethodException) {
                    val kotlinClass = eventClassToListenTo.kotlin
                    val companion = kotlinClass.companionObject
                    val instance = kotlinClass.companionObjectInstance
                    val property = companion!!.memberProperties.find { it.name == "listenersStatic" }!! as KProperty1<Any, Any>
                    val listeners = property.get(instance!!) as EventListeners
                    listeners
                }

                // Add our own to it
                thisEventKindOfListeners.registerListener(registeredListener)
                registeredEventListeners[thisEventKindOfListeners] = registeredListener

                // Depending on the event configuration we may or may not care about the
                // children events
                if (eventHandlerAnnotation.listenToChildEvents != EventHandler.ListenToChildEvents.NO)
                    addRegisteredListenerToEventChildren(thisEventKindOfListeners, registeredListener,
                            eventHandlerAnnotation.listenToChildEvents == EventHandler.ListenToChildEvents.RECURSIVE)

                logger().info("Successuflly added EventHandler for " + eventClassToListenTo.name + "in " + listener + " of plugin " + plugin)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
    }

    /*private fun addRegisteredListenerToEventChildren(listeners: EventListeners, registeredListener: RegisteredListener, recursive: Boolean) {
        for (el in listeners.childrens) {
            el.registerListener(registeredListener)
            registeredEventListeners[el] = registeredListener

            if (recursive)
                addRegisteredListenerToEventChildren(el, registeredListener, true)
        }
    }*/

    override fun fireEvent(event: Event) {
        /*val listeners = event.listeners

        for (listener in listeners.listeners) {
            try {
                listener.invokeForEvent(event)
            } catch (e: InvocationTargetException) {
                logger().warn("Exception while invoking event, in event handling body : " + e.targetException.message)
                // e.printStackTrace();
                e.targetException.printStackTrace()
            } catch (e: Exception) {
                logger().warn("Exception while invoking event : " + e.message)
                e.printStackTrace()
            }

        }

        // If we didn't surpress it's behaviour
        // if(event.isAllowedToExecute())
        // event.defaultBehaviour();*/
        // TODO()
        println("TODO: rework event system")
    }

    fun logger(): Logger {
        return pluginsLogger
    }

    fun commands(): Collection<Command> {
        return commands
    }

    override fun getPluginDirectory(plugin: Plugin): File {
        val file = File("." + "/" + plugin.name + "/")
        file.mkdirs()

        return file
    }

    companion object {
        private val pluginsLogger = LoggerFactory.getLogger("plugins")
    }
}
