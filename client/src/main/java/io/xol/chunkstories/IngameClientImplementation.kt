package io.xol.chunkstories

import io.xol.chunkstories.api.client.ClientInputsManager
import io.xol.chunkstories.api.client.ClientSoundManager
import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.client.LocalPlayer
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.graphics.Window
import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.particles.ParticlesManager
import io.xol.chunkstories.api.plugin.ClientPluginManager
import io.xol.chunkstories.api.plugin.PluginManager
import io.xol.chunkstories.api.util.Configuration
import io.xol.chunkstories.api.workers.Tasks
import io.xol.chunkstories.api.world.WorldClient
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.gui.layer.ingame.Ingame
import io.xol.chunkstories.world.WorldClientCommon
import org.slf4j.Logger

class IngameClientImplementation(val client: ClientImplementation, override val world : WorldClientCommon, override val player: LocalPlayer, val ingameGuiLayer : Ingame) : IngameClient {
    override val configuration: Configuration = client.configuration
    override val gameWindow: Window = client.gameWindow
    override val gui: Gui = client.gui
    override val inputsManager: ClientInputsManager = client.inputsManager
    override val pluginManager: ClientPluginManager = client.pluginManager
    override val soundManager: ClientSoundManager = client.soundManager
    override val content : Content = client.content
    override val tasks : Tasks = client.tasks

    override val decalsManager: DecalsManager = world.decalsManager
    override val ingame: IngameClient? = this
    override val particlesManager: ParticlesManager = world.particlesManager

    override fun changeWorld(world: WorldClient) {
        client.changeWorld(world)
    }

    override fun exitToMainMenu() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exitToMainMenu(errorMessage: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun logger(): Logger = client.logger()

    override fun print(message: String) {
        ingameGuiLayer.chatManager.insert(message)
        client.print(message)
    }
}