package io.xol.chunkstories.client.ingame

import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.client.ClientSlavePluginManager
import io.xol.chunkstories.world.WorldClientLocal
import io.xol.chunkstories.world.WorldClientRemote
import io.xol.chunkstories.world.WorldLoadingException
import io.xol.chunkstories.world.deserializeWorldInfo
import java.io.File

class IngameClientRemoteHost(client: ClientImplementation, worldInitializer: (IngameClientImplementation) -> WorldClientRemote) : IngameClientImplementation(client, worldInitializer) {
    override val world: WorldClientRemote = super.internalWorld as WorldClientRemote
    override val pluginManager: ClientSlavePluginManager = super.internalPluginManager as ClientSlavePluginManager
}