package xyz.chunkstories.client.ingame

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.net.ClientConnectionSequence
import xyz.chunkstories.client.net.ServerConnection
import xyz.chunkstories.gui.layer.ingame.ConnectingToServerUI
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldSubImplementation

fun ClientImplementation.connectToRemoteWorld(serverAddress: String, port: Int) {
    gui.topLayer = ConnectingToServerUI(gui, null, ClientConnectionSequence(this, serverAddress, port))
}

class IngameClientRemoteHost constructor(client: ClientImplementation, val connection: ServerConnection, worldInitializer: (IngameClientImplementation) -> WorldImplementation) : IngameClientImplementation(client, worldInitializer) {
    override val world: WorldSubImplementation
        get() = super.world_ as WorldSubImplementation
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