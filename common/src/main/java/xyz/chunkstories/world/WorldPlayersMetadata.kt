package xyz.chunkstories.world

import com.google.gson.Gson
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.api.world.World
import java.io.File

private val gson = Gson()

class WorldPlayersMetadata(val world: WorldMasterImplementation) {
    internal val map = mutableMapOf<PlayerID, PlayerSaveData>()

    operator fun get(id: PlayerID) = map[id]
}

fun WorldMasterImplementation.playerEnters(player: Player) {
    val playerEntityFile = File(folderPath + "/players/" + player.name.toLowerCase() + ".json")
    val data = if(playerEntityFile.exists()) {
        gson.fromJson(playerEntityFile.reader(), PlayerSaveData::class.java)
    } else {
        PlayerSaveData()
    }
    assert(!playersMetadata.map.contains(player.id))
    playersMetadata.map[player.id] = data
}

fun WorldMasterImplementation.playerLeaves(player: Player) {
    val playerEntityFile = File(folderPath + "/players/" + player.name.toLowerCase() + ".json")
    playerEntityFile.parentFile.mkdirs()
    val data = playersMetadata.map.remove(player.id)!!
    playerEntityFile.writeText(gson.toJson(data))
}

class PlayerSaveData {
    var deaths = 0
    var isSpectating = false
    var savedEntity: Json? = null
}