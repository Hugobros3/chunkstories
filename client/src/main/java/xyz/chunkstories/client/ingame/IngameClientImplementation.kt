package xyz.chunkstories.client.ingame

import org.slf4j.Logger
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.ContentTranslator
import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.commands.installClientCommands
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.vulkan.util.BuiltInRendergraphs
import xyz.chunkstories.gui.layer.MainMenuUI
import xyz.chunkstories.gui.layer.MessageBoxUI
import xyz.chunkstories.gui.layer.WorldLoadingUI
import xyz.chunkstories.gui.layer.ingame.IngameUI
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.server.commands.installHostCommands
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldMasterImplementation
import xyz.chunkstories.TickingThread

abstract class IngameClientImplementation protected constructor(val client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldImplementation) : IngameClient {
    override val engine: Client
        get() = client
    override val contentTranslator: ContentTranslator
        get() = world.contentTranslator
    override val logger: Logger
        get() = client.logger
    override val content: Content
        get() = client.content

    final override val pluginManager: DefaultPluginManager

    protected val world_: WorldImplementation = worldInitializer.invoke(this)
    abstract override val world: WorldImplementation
    final override val player: ClientPlayer

    val tickingThread: TickingThread
    val loadingAgent = LocalClientLoadingAgent(this)

    val decalsManager: DecalsManager
    val particlesManager: ParticlesManager

    val ingameUI: IngameUI
    val worldRenderer: WorldRenderer

    init {
        decalsManager = world_.decalsManager
        particlesManager = world_.particlesManager

        // We need the plugin manager very early so we make it in the common abstract class constructor
        pluginManager = DefaultPluginManager(this)
        pluginManager.reloadPlugins()

        // Prepare command line
        installClientCommands(this)
        if (this is Host) {
            installHostCommands(this)
        }

        player = ClientPlayer(this)

        client.ingame = this

        worldRenderer = client.gameWindow.graphicsEngine.backend.createWorldRenderer(world_)
        ingameUI = IngameUI(client.gui, this)

        tickingThread = object : TickingThread(world_) {
            override fun tick() {
                onTick()
            }
        }
    }

    open fun onceCreated() {
        // Spawn manually the player if we're in single player mode
        if (world_ is WorldMasterImplementation) {
            client.gui.topLayer = WorldLoadingUI(world_, this, client.gui, ingameUI)
        } else {
            client.gui.topLayer = ingameUI
        }

       tickingThread.start()
        //     internalWorld.spawnPlayer(player)

        /*val connectionProgressLayer = gui.topLayer as? ConnectingScreen
        if (connectionProgressLayer != null) //TODO generalize to other loading hider overlays
            TODO() //connectionProgressLayer.parentLayer = ingameGuiLayer
        else*/
    }

    open fun onFrame() {
        player.onFrame()
    }

    open fun onTick() {
        player.onTick()
        tickingThread.tickWorld()
    }

    override fun exitToMainMenu() {
        destroy()
        client.gui.topLayer = MainMenuUI(client.gui, null)
    }

    override fun exitToMainMenu(errorMessage: String) {
        destroy()
        client.gui.topLayer = MessageBoxUI(client.gui, MainMenuUI(client.gui, null), "Disconnected from server", errorMessage)
    }

    protected open fun destroy() {
        logger.info("Removing player from world")
        player.destroy()

        logger.info("Terminating ticking thread...")
        tickingThread.terminate().traverse()
        logger.info("Terminating ticking thread... done")
        pluginManager.disablePlugins()
        logger.info("Disabled plugins")
        worldRenderer.cleanup()
        logger.debug("Destroyed world renderer")
        loadingAgent.unloadEverything(true)
        logger.debug("Unloaded world bits")
        world_.destroy()
        client.soundManager.stopAllSounds()

        logger.debug("Left ingame state")
        client.gameWindow.graphicsEngine.loadRenderGraph(BuiltInRendergraphs.onlyGuiRenderGraph(client))
        client.ingame = null
    }

    fun print(message: String) {
        ingameUI.chatManager.insert(message)
        client.chatLogger.info(message)
    }
}