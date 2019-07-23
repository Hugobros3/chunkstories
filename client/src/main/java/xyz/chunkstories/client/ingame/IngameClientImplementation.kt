package xyz.chunkstories.client.ingame

import org.slf4j.Logger
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.api.workers.Tasks
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.commands.installClientCommands
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.vulkan.util.BuiltInRendergraphs
import xyz.chunkstories.gui.layer.MainMenu
import xyz.chunkstories.gui.layer.MessageBox
import xyz.chunkstories.gui.layer.WorldLoadingUI
import xyz.chunkstories.gui.layer.ingame.IngameLayer
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.server.commands.installServerCommands
import xyz.chunkstories.task.WorkerThreadPool
import xyz.chunkstories.world.WorldClientCommon
import xyz.chunkstories.world.WorldClientLocal

abstract class IngameClientImplementation protected constructor(val client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientCommon) : IngameClient, Client by client {
    final override val tasks: WorkerThreadPool = client.tasks

    final override val ingame: IngameClient? = this

    val internalPluginManager: DefaultPluginManager
    val internalWorld: WorldClientCommon = worldInitializer.invoke(this)

    abstract override val world: WorldClientCommon

    final override val decalsManager: DecalsManager
    final override val particlesManager: ParticlesManager

    final override val player: LocalPlayerImplementation

    val ingameGuiLayer: IngameLayer

    val worldRenderer: WorldRenderer

    init {
        decalsManager = internalWorld.decalsManager
        particlesManager = internalWorld.particlesManager

        // We need the plugin manager very early so we make it in the common abstract class constructor
        internalPluginManager = DefaultPluginManager(this)
        internalPluginManager.reloadPlugins()

        // Prepare command line
        installClientCommands(this)
        if (this is IngameClientLocalHost) {
            installServerCommands(this)
        }

        player = LocalPlayerImplementation(this, internalWorld)

        client.ingame = this

        worldRenderer = client.gameWindow.graphicsEngine.backend.createWorldRenderer(internalWorld)

        ingameGuiLayer = IngameLayer(gui, this)
        // Spawn manually the player if we're in single player mode
        if (internalWorld is WorldClientLocal) {
            gui.topLayer = WorldLoadingUI(internalWorld, gui, ingameGuiLayer)
        } else {
            gui.topLayer = ingameGuiLayer
        }
        //     internalWorld.spawnPlayer(player)

        /*val connectionProgressLayer = gui.topLayer as? ConnectingScreen
        if (connectionProgressLayer != null) //TODO generalize to other loading hider overlays
            TODO() //connectionProgressLayer.parentLayer = ingameGuiLayer
        else*/
    }

    override fun exitToMainMenu() {
        exitCommon()

        gui.topLayer = MainMenu(gui, null)
        soundManager.stopAnySound()
        client.ingame = null
    }

    override fun exitToMainMenu(errorMessage: String) {
        exitCommon()

        gui.topLayer = MessageBox(gui, MainMenu(gui, null), "Disconnected from server", errorMessage)
        soundManager.stopAnySound()
        client.ingame = null
    }

    open fun exitCommon() {
        pluginManager.disablePlugins()

        worldRenderer.cleanup()
        client.gameWindow.graphicsEngine.loadRenderGraph(BuiltInRendergraphs.onlyGuiRenderGraph(client))

        // Destroy the world
        internalWorld.destroy()
    }

    override fun logger(): Logger = client.logger()

    override fun print(message: String) {
        ingameGuiLayer.chatManager.insert(message)
        client.print(message)
    }
}