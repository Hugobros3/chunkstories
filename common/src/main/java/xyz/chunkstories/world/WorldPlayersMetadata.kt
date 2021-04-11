package xyz.chunkstories.world

import com.google.gson.Gson
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.PlayerID
import java.io.File

class WorldPlayersMetadata(val world: WorldImplementation) {
    private val gson = Gson()
    private val map = mutableMapOf<PlayerID, PlayerSaveData>()

    fun playerEnters(player: Player) {
        val playerEntityFile = File(world.folderPath + "/players/" + player.name.toLowerCase() + ".json")
        val data = if(playerEntityFile.exists()) {
            gson.fromJson(playerEntityFile.reader(), PlayerSaveData::class.java)
        } else {
            PlayerSaveData()
        }
        map[player.id] = data
    }

    fun playerLeaves(player: Player) {
        val playerEntityFile = File(world.folderPath + "/players/" + player.name.toLowerCase() + ".json")
        playerEntityFile.parentFile.mkdirs()
        val data = map.remove(player.id)!!
        playerEntityFile.writeText(gson.toJson(data))
    }

    operator fun get(id: PlayerID) = map[id]
}

class PlayerSaveData {
    var deaths = 0
    var isSpectating = false
    var savedEntity: Json? = null
}