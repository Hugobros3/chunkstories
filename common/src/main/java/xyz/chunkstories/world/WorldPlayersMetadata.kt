package xyz.chunkstories.world

import com.google.gson.Gson
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.player.Player
import java.io.File

class WorldPlayersMetadata(val world: WorldImplementation) {
    private val gson = Gson()
    private val map = mutableMapOf<Player, WorldPlayerMetadata>()

    fun playerEnters(player: Player) {
        val playerEntityFile = File(world.folderPath + "/players/" + player.name.toLowerCase() + ".json")
        val data = if(playerEntityFile.exists()) {
            gson.fromJson(playerEntityFile.reader(), WorldPlayerMetadata::class.java)
        } else {
            WorldPlayerMetadata()
        }
        map[player] = data
    }

    fun playerLeaves(player: Player) {
        val playerEntityFile = File(world.folderPath + "/players/" + player.name.toLowerCase() + ".json")
        playerEntityFile.parentFile.mkdirs()
        val data = map.remove(player)!!
        playerEntityFile.writeText(gson.toJson(data))
    }

    operator fun get(player: Player) = map[player]
}

class WorldPlayerMetadata {
    var savedEntity: Json? = null
    var deaths = 0
}