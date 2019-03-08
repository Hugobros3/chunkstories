package xyz.chunkstories.client.ingame

import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.plugin.DefaultPluginManager
import xyz.chunkstories.world.WorldClientRemote

fun ClientImplementation.connectToRemoteWorld(address: String, port: Int) : WorldClientRemote {
    TODO()
}

class IngameClientRemoteHost(client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientRemote) : IngameClientImplementation(client, worldInitializer) {
    override val world: WorldClientRemote = super.internalWorld as WorldClientRemote
    override val pluginManager: DefaultPluginManager = super.internalPluginManager
}