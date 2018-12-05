package io.xol.chunkstories.client.ingame

import io.xol.chunkstories.api.client.ClientIdentity
import io.xol.chunkstories.api.client.ClientInputsManager
import io.xol.chunkstories.api.client.ClientSoundManager
import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.graphics.Window
import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.particles.ParticlesManager
import io.xol.chunkstories.api.plugin.ClientPluginManager
import io.xol.chunkstories.api.util.configuration.Configuration
import io.xol.chunkstories.api.workers.Tasks
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.client.ClientMasterPluginManager
import io.xol.chunkstories.client.ClientSlavePluginManager
import io.xol.chunkstories.graphics.vulkan.util.BuiltInRendergraphs
import io.xol.chunkstories.gui.layer.MainMenu
import io.xol.chunkstories.gui.layer.MessageBox
import io.xol.chunkstories.gui.layer.SkyBoxBackground
import io.xol.chunkstories.gui.layer.ingame.IngameLayer
import io.xol.chunkstories.gui.layer.ingame.RemoteConnectionGuiLayer
import io.xol.chunkstories.server.commands.InstallServerCommands
import io.xol.chunkstories.client.commands.ReloadContentCommand
import io.xol.chunkstories.client.commands.installClientCommands
import io.xol.chunkstories.world.WorldClientCommon
import org.slf4j.Logger

abstract class IngameClientImplementation protected constructor(val client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientCommon) : IngameClient {
    final override val configuration: Configuration = client.configuration
    final override val gameWindow: Window = client.gameWindow
    final override val gui: Gui = client.gui
    final override val inputsManager: ClientInputsManager = client.inputsManager
    final override val soundManager: ClientSoundManager = client.soundManager
    final override val content: Content = client.content
    final override val tasks: Tasks = client.tasks
    final override val user: ClientIdentity = client.user

    final override val ingame: IngameClient? = this

    val internalPluginManager: ClientPluginManager
    val internalWorld: WorldClientCommon = worldInitializer.invoke(this)

    abstract override val world: WorldClientCommon

    final override val decalsManager: DecalsManager
    final override val particlesManager: ParticlesManager

    final override val player: LocalPlayerImplementation

    val ingameGuiLayer: IngameLayer

    init {
        decalsManager = internalWorld.decalsManager
        particlesManager = internalWorld.particlesManager

        // We need the plugin manager very early so we make it in the common abstract class constructor
        internalPluginManager = (this as? IngameClientLocalHost)?.let { ClientMasterPluginManager(it) } ?: ClientSlavePluginManager(this)
        internalPluginManager.reloadPlugins()

        // Prepare command line
        installClientCommands(this)
        if (this is IngameClientLocalHost) {
            InstallServerCommands(this)
        }

        player = LocalPlayerImplementation(this, internalWorld)

        // Spawn manually the player if we're in single player mode
        if (internalWorld is WorldMaster)
            internalWorld.spawnPlayer(player)

        client.ingame = this
        client.gameWindow.graphicsBackend.queuedRenderGraph = BuiltInRendergraphs.debugRenderGraph

        ingameGuiLayer = IngameLayer(gui, this)
        val connectionProgressLayer = gui.topLayer as? RemoteConnectionGuiLayer
        if (connectionProgressLayer != null) //TODO generalize to other loading hider overlays
            connectionProgressLayer.parentLayer = ingameGuiLayer
        else
            gui.topLayer = ingameGuiLayer

        // Start only the logic after all that
        internalWorld.startLogic()
    }

    override fun exitToMainMenu() {
        exitCommon()

        gui.topLayer = MainMenu(gui, SkyBoxBackground(gui))
        soundManager.stopAnySound()
        client.ingame = null
    }

    override fun exitToMainMenu(errorMessage: String) {
        exitCommon()

        gui.topLayer = MessageBox(gui, SkyBoxBackground(gui), errorMessage)
        soundManager.stopAnySound()
        client.ingame = null
    }

    open fun exitCommon() {
        pluginManager.disablePlugins()

        client.gameWindow.graphicsBackend.queuedRenderGraph = BuiltInRendergraphs.onlyGuiRenderGraph

        // Destroy the world
        internalWorld.destroy()
    }

    override fun logger(): Logger = client.logger()

    override fun print(message: String) {
        ingameGuiLayer.chatManager.insert(message)
        client.print(message)
    }
}