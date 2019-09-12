package xyz.chunkstories.world

import xyz.chunkstories.api.world.WorldInfo
import java.io.File
import java.util.*
import kotlin.streams.toList

fun createWorld(folder: File, worldInfo: WorldInfo) {
    folder.mkdirs()
    val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldInfoFilename)
    worldInfoFile.writeText(serializeWorldInfo(worldInfo, true))

    val internalData = WorldInternalData()
    val random = Random((worldInfo.seed + "_spawn").codePoints().toList().reduce(Int::xor).toLong())
    val randomWeather = random.nextFloat()
    internalData.weather = randomWeather
    val spawnCoordinateX = random.nextInt(worldInfo.size.sizeInChunks * 32)
    val spawnCoordinateZ = random.nextInt(worldInfo.size.sizeInChunks * 32)
    internalData.spawnLocation.set(spawnCoordinateX + 0.5, 64.0, spawnCoordinateZ + 0.5)

    val internalDataFile = File(folder.path + "/" + WorldImplementation.worldInternalDataFilename)
    internalData.writeToDisk(internalDataFile)
}