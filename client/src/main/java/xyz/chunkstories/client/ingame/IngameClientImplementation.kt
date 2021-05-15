package xyz.chunkstories.client.ingame

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.IngameClient
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
import xyz.chunkstories.sound.ALSoundManager
import xyz.chunkstories.task.WorkerThreadPool
import xyz.chunkstories.util.alias
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldMasterImplementation

abstract class IngameClientImplementation protected constructor(val client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldImplementation) : IngameClient, Client by client {
    override val engine: Client
        get() = client
    override val contentTranslator: ContentTranslator
        get() = world.contentTranslator
    override val logger: Logger
        get() = client.logger
    override val tasks: WorkerThreadPool
        get() = client.tasks

    final override val ingame: IngameClient = this
    final override val soundManager: ALSoundManager by alias(client::soundManager)
    final override val pluginManager: DefaultPluginManager

    protected val world_: WorldImplementation = worldInitializer.invoke(this)
    abstract override val world: WorldImplementation
    final override val player: ClientPlayer

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
        ingameUI = IngameUI(gui, this)
    }

    open fun onceCreated() {
        // Spawn manually the player if we're in single player mode
        if (world_ is WorldMasterImplementation) {
            gui.topLayer = WorldLoadingUI(world_, this, gui, ingameUI)
        } else {
            gui.topLayer = ingameUI
        }
        //     internalWorld.spawnPlayer(player)

        /*val connectionProgressLayer = gui.topLayer as? ConnectingScreen
        if (connectionProgressLayer != null) //TODO generalize to other loading hider overlays
            TODO() //connectionProgressLayer.parentLayer = ingameGuiLayer
        else*/
    }

    override fun exitToMainMenu() {
        exitCommon()
        gui.topLayer = MainMenuUI(gui, null)
    }

    override fun exitToMainMenu(errorMessage: String) {
        exitCommon()
        gui.topLayer = MessageBoxUI(gui, MainMenuUI(gui, null), "Disconnected from server", errorMessage)
    }

    open fun exitCommon() {
        pluginManager.disablePlugins()

        worldRenderer.cleanup()
        loadingAgent.unloadEverything(true)
        world_.destroy()

        soundManager.stopAllSounds()
        client.gameWindow.graphicsEngine.loadRenderGraph(BuiltInRendergraphs.onlyGuiRenderGraph(client))

        client.ingame = null
    }

    fun print(message: String) {
        ingameUI.chatManager.insert(message)
        client.chatLogger.info(message)
    }
}