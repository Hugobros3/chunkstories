package xyz.chunkstories.world

import xyz.chunkstories.api.world.WorldInfo
import java.io.File
import java.util.*

fun createWorld(folder: File, worldInfo: WorldInfo) {
    folder.mkdirs()
    val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldInfoFilename)
    worldInfoFile.writeText(serializeWorldInfo(worldInfo, true))

    val internalData = WorldInternalData()
    val seedAsByteArray = (worldInfo.seed + "_spawn").toByteArray()
    var i = 0
    var processedSeed: Long = 0
    while (i < 512) {
        for (j in 0..7)
            processedSeed = processedSeed xor ((seedAsByteArray[(i * 3 + j) % seedAsByteArray.size].toLong().shl(j * 8)))
        i += 8
    }

    //val processedSeed = (worldInfo.seed + "_spawn").toCharArray().map { it.toInt() }.toList().reduce(Int::xor).toLong()
    val random = Random(processedSeed)
    val randomWeather = random.nextFloat()
    internalData.weather = randomWeather * randomWeather // bias towards sunny
    println("WEATHER GENERATE: $randomWeather $processedSeed ${worldInfo.seed}")
    val spawnCoordinateX = random.nextInt(worldInfo.size.sizeInChunks * 32)
    val spawnCoordinateZ = random.nextInt(worldInfo.size.sizeInChunks * 32)
    internalData.spawnLocation.set(spawnCoordinateX + 0.5, 64.0, spawnCoordinateZ + 0.5)

    val internalDataFile = File(folder.path + "/" + WorldImplementation.worldInternalDataFilename)
    internalData.writeToDisk(internalDataFile)
}