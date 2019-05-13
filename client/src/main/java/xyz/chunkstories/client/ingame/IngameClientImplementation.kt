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
import xyz.chunkstories.gui.layer.ingame.IngameLayer
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.server.commands.installServerCommands
import xyz.chunkstories.world.WorldClientCommon

abstract class IngameClientImplementation protected constructor(val client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientCommon) : IngameClient, Client by client {
    //final override val configuration: Configuration = client.configuration
    //final override val gameWindow: Window = client.gameWindow
    //final override val graphics: GraphicsEngine = client.graphics
    //final override val gui: Gui = client.gui
    //final override val inputsManager: ClientInputsManager = client.inputsManager
    //final override val soundManager: ClientSoundManager = client.soundManager
    //final override val content: Content = client.content
    final override val tasks: Tasks = client.tasks
    //final override val user: ClientIdentity = client.user

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

        // Spawn manually the player if we're in single player mode
        if (internalWorld is WorldMaster)
            internalWorld.spawnPlayer(player)

        client.ingame = this

        worldRenderer = client.gameWindow.graphicsEngine.backend.createWorldRenderer(internalWorld)

        ingameGuiLayer = IngameLayer(gui, this)
        /*val connectionProgressLayer = gui.topLayer as? RemoteConnectionGuiLayer
        if (connectionProgressLayer != null) //TODO generalize to other loading hider overlays
            TODO() //connectionProgressLayer.parentLayer = ingameGuiLayer
        else*/
        gui.topLayer = ingameGuiLayer

        // Start only the logic after all that
        internalWorld.startLogic()
    }

    override fun exitToMainMenu() {
        exitCommon()

        gui.topLayer = MainMenu(gui, null)
        soundManager.stopAnySound()
        client.ingame = null
    }

    override fun exitToMainMenu(errorMessage: String) {
        exitCommon()

        gui.topLayer = MessageBox(gui, null, errorMessage)
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