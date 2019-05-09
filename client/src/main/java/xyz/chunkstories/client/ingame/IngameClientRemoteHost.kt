package xyz.chunkstories.client.ingame

import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.net.ClientConnectionSequence
import xyz.chunkstories.client.net.ServerConnection
import xyz.chunkstories.gui.layer.ingame.RemoteConnectionGuiLayer
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.world.WorldClientRemote

fun ClientImplementation.connectToRemoteWorld(serverAddress: String, port: Int) {
    gui.topLayer = RemoteConnectionGuiLayer(gui, null, ClientConnectionSequence(this, serverAddress, port))
}

class IngameClientRemoteHost(client: ClientImplementation, val connection: ServerConnection, worldInitializer: (IngameClientImplementation) -> WorldClientRemote) : IngameClientImplementation(client, worldInitializer) {
    override val world: WorldClientRemote = super.internalWorld as WorldClientRemote
    override val pluginManager: DefaultPluginManager
        get() = super.internalPluginManager

    override fun exitCommon() {
        connection.close()
        super.exitCommon()
    }
}