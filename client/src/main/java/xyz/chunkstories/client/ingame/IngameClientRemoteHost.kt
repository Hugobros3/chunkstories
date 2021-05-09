package xyz.chunkstories.client.ingame

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.ContentTranslator
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.net.ClientConnectionSequence
import xyz.chunkstories.client.net.ServerConnection
import xyz.chunkstories.gui.layer.ingame.ConnectingToServerUI
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.world.WorldClientRemote
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldSubImplementation

fun ClientImplementation.connectToRemoteWorld(serverAddress: String, port: Int) {
    gui.topLayer = ConnectingToServerUI(gui, null, ClientConnectionSequence(this, serverAddress, port))
}

class IngameClientRemoteHost(client: ClientImplementation, val connection: ServerConnection, worldInitializer: (IngameClientImplementation) -> WorldImplementation) : IngameClientImplementation(client, worldInitializer) {
    override val world: WorldSubImplementation = super.world_ as WorldSubImplementation

    override val contentTranslator: ContentTranslator
        get() = world.contentTranslator
    override val logger: Logger = LoggerFactory.getLogger("client.world")

    override fun startPlayingAs_(entity: Entity) {
        TODO("Not yet implemented")
    }

    override fun startSpectating_() {
        TODO("Not yet implemented")
    }

    override fun exitCommon() {
        connection.close("Exiting world")
        super.exitCommon()
    }
}