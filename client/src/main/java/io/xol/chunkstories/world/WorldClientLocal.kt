//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world

import java.io.File

import io.xol.chunkstories.api.player.Player
import io.xol.chunkstories.api.sound.SoundManager
import io.xol.chunkstories.api.util.IterableIterator
import io.xol.chunkstories.api.world.WorldInfo
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.client.ingame.IngameClientLocalHost
import io.xol.chunkstories.world.io.IOTasks

class WorldClientLocal @Throws(WorldLoadingException::class)
constructor(val localHost: IngameClientLocalHost, info: WorldInfo, folder: File) : WorldClientCommon(localHost, info, null, folder), WorldMaster {

    override val soundManager: SoundManager
        get() = client.soundManager

    override val ioHandler = IOTasks(this)

    init {
        ioHandler.start()
    }

    override fun tick() {
        // TODO: processIncommingPackets();
        // TODO: flush getAllVoxelComponents
        super.tick()
    }

    override val players: IterableIterator<Player> = localHost.connectedPlayers

    override fun getPlayerByName(playerName: String): Player? = localHost.getPlayerByName(playerName)
}
