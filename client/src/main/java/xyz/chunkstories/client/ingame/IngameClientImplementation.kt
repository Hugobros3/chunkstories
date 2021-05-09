package xyz.chunkstories.client.ingame

import org.slf4j.Logger
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.IngameClient
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
    val tasks: WorkerThreadPool = client.tasks

    final override val ingame: IngameClient = this
    final override val soundManager: ALSoundManager by alias(client::soundManager)
    final override val pluginManager: DefaultPluginManager

    val loadingAgent = LocalClientLoadingAgent(this)

    val world_: WorldImplementation = worldInitializer.invoke(this)
    abstract override val world: WorldImplementation

    val decalsManager: DecalsManager
    val particlesManager: ParticlesManager

    final override val player: LocalPlayerImplementation

    val ingameGuiUI: IngameUI

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

        player = LocalPlayerImplementation(this)

        client.ingame = this

        worldRenderer = client.gameWindow.graphicsEngine.backend.createWorldRenderer(world_)

        ingameGuiUI = IngameUI(gui, this)
        // Spawn manually the player if we're in single player mode
        if (world_ is WorldMasterImplementation) {
            gui.topLayer = WorldLoadingUI(world_, this, gui, ingameGuiUI)
        } else {
            gui.topLayer = ingameGuiUI
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

    fun logger(): Logger = client.logger

    fun print(message: String) {
        ingameGuiUI.chatManager.insert(message)
        client.chatLogger.info(message)
    }
}