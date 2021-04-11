package xyz.chunkstories

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.world.WorldImplementation

/*
abstract class PlayerCommon(final override val name: String) : Player {
    fun eventEntersWorld(world: World) {
        if (world is WorldMaster) {
            (world as WorldImplementation).playersMetadata.playerEnters(this)
        }
    }

    fun eventLeavesWorld(world: World) {
        if (world is WorldMaster) {
            (world as WorldImplementation).playersMetadata.playerLeaves(this)
        }
    }
}*/