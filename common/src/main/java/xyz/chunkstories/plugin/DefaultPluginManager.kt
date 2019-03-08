//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.plugin

import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Deque
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import kotlin.collections.Map.Entry
import java.util.concurrent.LinkedBlockingDeque

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.events.Event
import xyz.chunkstories.api.events.EventExecutor
import xyz.chunkstories.api.events.EventHandler
import xyz.chunkstories.api.events.EventListeners
import xyz.chunkstories.api.events.Listener
import xyz.chunkstories.api.events.RegisteredListener
import xyz.chunkstories.api.exceptions.plugins.PluginCreationException
import xyz.chunkstories.api.exceptions.plugins.PluginLoadException
import xyz.chunkstories.api.plugin.ChunkStoriesPlugin
import xyz.chunkstories.api.plugin.PluginInformation
import xyz.chunkstories.api.plugin.PluginInformation.PluginType
import xyz.chunkstories.api.plugin.PluginManager
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.plugin.commands.CommandHandler
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.content.GameDirectory
import xyz.chunkstories.content.mods.ModsManagerImplementation

open class DefaultPluginManager(private val pluginExecutionContext: GameContext) : PluginManager {
    var activePlugins: MutableSet<ChunkStoriesPlugin> = HashSet()
    private val registeredEventListeners = HashMap<EventListeners, RegisteredListener>()

    var commandsAliases: MutableMap<String, Command> = HashMap()
    var commands: MutableSet<Command> = HashSet()

    override fun activePlugins(): Collection<ChunkStoriesPlugin> {
        return activePlugins
    }

    override fun reloadPlugins() {
        // First disable any leftover plugins
        disablePlugins()

        // We make a list
        val pluginsToLoad = mutableListOf<PluginInformation>()

        // Creates plugins folder if it isn't present.
        val pluginsFolder = File(GameDirectory.getGameFolderPath() + "/plugins/")
        pluginsFolder.mkdirs()

        // Iterates over files of the folder
        for (file in pluginsFolder.listFiles()!!) {
            if (file.name.endsWith(".jar")) {
                try {
                    val pluginInformation: PluginInformation = loadPluginInfo(file) ?: continue

                    // Checks type is appropriate
                    if (pluginInformation.pluginType == PluginType.CLIENT_ONLY && pluginExecutionContext !is Client ||
                            pluginInformation.pluginType == PluginType.MASTER && pluginExecutionContext !is Server)
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
        for (pluginInformation in (this.pluginExecutionContext.content.modsManager() as ModsManagerImplementation).pluginsWithinEnabledMods) {

            // Checks type is appropriate
            if (pluginInformation.pluginType == PluginType.CLIENT_ONLY && pluginExecutionContext !is Client ||
                    pluginInformation.pluginType == PluginType.MASTER && pluginExecutionContext !is Server)
                continue

            pluginsToLoad.add(pluginInformation)
        }

        // Enables the found plugins
        enablePlugins(pluginsToLoad)
    }

    private fun enablePlugins(pluginsToInitialize: List<PluginInformation>) {
        logger().info(pluginsToInitialize.size.toString() + " plugins to initialize")
        val finalClassLoader = (pluginExecutionContext.content.modsManager() as ModsManagerImplementation).finalClassLoader!!

        val order = LinkedBlockingDeque<PluginInformation>()

        // TODO sort plugins requirements (requires/before)
        for (pluginInformation in pluginsToInitialize) {
            order.add(pluginInformation)
        }

        // Loads each provided plugin
        for (pluginInformation in order) {
            try {
                // Add commands support
                /*for (Command command : pluginInformation.getCommands()) {
                    // Checks the command isn't already defined
                    if (commands.contains(command)) {
                        logger().warn("Plugin " + pluginInformation.getName() + " can't define the command "
                                + command.getName() + " as it's already defined by another plugin.");
                        continue;
                    }

                    commands.add(command);

                    for (String alias : command.aliases())
                        if (commandsAliases.put(alias, command) != null)
                            logger().warn("Plugin " + pluginInformation + " tried to register alias " + alias
                                    + " for command " + command + ".");

                }*/

                // Instanciate the plugin after all
                val pluginInstance = pluginInformation.createInstance(pluginExecutionContext, finalClassLoader)

                activePlugins.add(pluginInstance)
                pluginInstance.onEnable()
            } catch (pce: PluginCreationException) {
                logger().error("Couldn't create plugin " + pluginInformation + " : " + pce.message)
                pce.printStackTrace()
            }

        }

    }

    override fun disablePlugins() {
        // Call onDisable for plugins
        for (plugin in activePlugins)
            plugin.onDisable()

        // Remove one by one each listener
        for ((key, value) in registeredEventListeners) {
            key.unRegisterListener(value)
        }

        // Remove registered commands
        // TODO only remove plugins commands
        commandsAliases.clear()
        commands.clear()

        // At last clear the plugins list
        activePlugins.clear()
    }

    override fun registerCommand(commandName: String, handler: CommandHandler, vararg aliases: String): Command {
        val command = Command(commandName)
        command.handler = handler

        for (alias in aliases) {
            command.addAlias(alias)
            commandsAliases[alias] = command
        }

        this.commands.add(command)
        commandsAliases[commandName] = command

        return command
    }

    override fun unregisterCommand(command: Command) {
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

    override fun registerEventListener(listener: Listener, plugin: ChunkStoriesPlugin) {
        println("Registering $listener")
        try {
            // Get a list of all the classes methods
            val methods = HashSet<Method>()
            for (method in listener.javaClass.methods)
                methods.add(method)
            for (method in listener.javaClass.declaredMethods)
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
                val parameter = method.parameterTypes[0].asSubclass(Event::class.java)
                // Create an EventExecutor to launch the event code
                val executor = EventExecutor { event -> method.invoke(listener, event) }
                val registeredListener = RegisteredListener(listener, plugin, executor,
                        eventHandlerAnnotation.priority)

                // Get the listeners list for this event
                val getListeners = parameter.getMethod("getListenersStatic")
                getListeners.isAccessible = true
                val thisEventKindOfListeners = getListeners.invoke(null) as EventListeners

                // Add our own to it
                thisEventKindOfListeners.registerListener(registeredListener)
                registeredEventListeners[thisEventKindOfListeners] = registeredListener

                // Depending on the event configuration we may or may not care about the
                // children events
                if (eventHandlerAnnotation.listenToChildEvents != EventHandler.ListenToChildEvents.NO)
                    addRegisteredListenerToEventChildren(thisEventKindOfListeners, registeredListener,
                            eventHandlerAnnotation.listenToChildEvents == EventHandler.ListenToChildEvents.RECURSIVE)

                logger().info("Successuflly added EventHandler for " + parameter.name + "in " + listener
                        + " of plugin " + plugin)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun addRegisteredListenerToEventChildren(listeners: EventListeners, registeredListener: RegisteredListener,
                                                     recursive: Boolean) {
        for (el in listeners.childrens) {
            el.registerListener(registeredListener)
            registeredEventListeners[el] = registeredListener

            if (recursive)
                addRegisteredListenerToEventChildren(el, registeredListener, true)
        }
    }

    override fun fireEvent(event: Event) {
        val listeners = event.listeners

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
        // event.defaultBehaviour();
    }

    fun logger(): Logger {
        return pluginsLogger
    }

    override fun commands(): Collection<Command> {
        return commands
    }

    override fun getPluginDirectory(plugin: ChunkStoriesPlugin): File {
        val file = File(GameDirectory.getGameFolderPath() + "/" + plugin.name + "/")
        file.mkdirs()

        return file
    }

    companion object {
        private val pluginsLogger = LoggerFactory.getLogger("plugins")
    }
}
