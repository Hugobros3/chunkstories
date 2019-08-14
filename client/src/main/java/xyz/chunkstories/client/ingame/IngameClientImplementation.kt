package xyz.chunkstories.client.ingame

import org.slf4j.Logger
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.commands.installClientCommands
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.vulkan.util.BuiltInRendergraphs
import xyz.chunkstories.gui.layer.MainMenuUI
import xyz.chunkstories.gui.layer.MessageBoxUI
import xyz.chunkstories.gui.layer.WorldLoadingUI
import xyz.chunkstories.gui.layer.ingame.IngameUI
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

    val ingameGuiUI: IngameUI

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

        ingameGuiUI = IngameUI(gui, this)
        // Spawn manually the player if we're in single player mode
        if (internalWorld is WorldClientLocal) {
            gui.topLayer = WorldLoadingUI(internalWorld, gui, ingameGuiUI)
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
        soundManager.stopAnySound()
        client.ingame = null
    }

    override fun exitToMainMenu(errorMessage: String) {
        exitCommon()

        gui.topLayer = MessageBoxUI(gui, MainMenuUI(gui, null), "Disconnected from server", errorMessage)
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
        ingameGuiUI.chatManager.insert(message)
        client.print(message)
    }
}