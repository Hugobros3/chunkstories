package xyz.chunkstories.client.ingame

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.ClientSlavePluginManager
import xyz.chunkstories.world.WorldClientLocal
import xyz.chunkstories.world.WorldClientRemote
import xyz.chunkstories.world.WorldLoadingException
import xyz.chunkstories.world.deserializeWorldInfo
import java.io.File

fun ClientImplementation.connectToRemoteWorld(address: String, port: Int) : WorldClientRemote {
    TODO()
}

class IngameClientRemoteHost(client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientRemote) : IngameClientImplementation(client, worldInitializer) {
    override val world: WorldClientRemote = super.internalWorld as WorldClientRemote
    override val pluginManager: ClientSlavePluginManager = super.internalPluginManager as ClientSlavePluginManager
}