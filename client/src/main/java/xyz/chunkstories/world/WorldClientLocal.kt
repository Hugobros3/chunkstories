//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

import java.io.File

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.util.IterableIterator
import xyz.chunkstories.api.world.WorldInfo
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.client.ingame.IngameClientLocalHost
import xyz.chunkstories.world.io.IOTasks

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
        // TODO: flush all
        super.tick()
    }

    override val players: Set<Player> = localHost.connectedPlayers

    override fun getPlayerByName(playerName: String): Player? = localHost.getPlayerByName(playerName)
}
