//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

@file:Suppress("NAME_SHADOWING")

package xyz.chunkstories.world

import com.google.gson.Gson
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.block.BlockAdditionalData
import xyz.chunkstories.api.block.structures.Prefab
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.util.concurrency.Fence
import xyz.chunkstories.api.entity.EntityID
import xyz.chunkstories.api.entity.Subscriber
import xyz.chunkstories.api.events.world.WorldTickEvent
import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.api.math.MathUtils.ceil
import xyz.chunkstories.api.math.MathUtils.floor
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.net.packets.PacketEntity
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.PlayerState
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.world.*
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.translator.AbstractContentTranslator
import xyz.chunkstories.content.translator.InitialContentTranslator
import xyz.chunkstories.content.translator.LoadedContentTranslator
import xyz.chunkstories.net.Connection
import xyz.chunkstories.net.LogicalPacketDatagram
import xyz.chunkstories.util.alias
import xyz.chunkstories.util.concurrency.CompoundFence
import xyz.chunkstories.world.chunk.ChunksStorage
import xyz.chunkstories.world.heightmap.HeightmapsStorage
import xyz.chunkstories.world.io.IOTasks
import xyz.chunkstories.world.region.RegionsStorage
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val TPS = 60

class WorldLoadingException(message: String, cause: Throwable? = null) : Exception(message, cause)

sealed class WorldImplementation constructor(
        override val gameInstance: GameInstance,
        final override var properties: World.Properties,
        var internalData: WorldInternalData,
        val contentTranslator: AbstractContentTranslator)
    : World {

    final override val generator: WorldGenerator

    open val ioThread = IOTasks(this)

    override val chunksManager by lazy { ChunksStorage(this) }
    override val regionsManager by lazy { RegionsStorage(this) }
    override val heightmapsManager by lazy { HeightmapsStorage(this) }

    //TODO go through the code & change write locks into update locks where possible
    val entitiesLock: ReadWriteLock = ReentrantReadWriteUpdateLock()
    val entities_ = mutableListOf<Entity>()

    override val collisionsManager by lazy { DefaultWorldCollisionsManager(this) }

    val internalDataLock = ReentrantLock()

    override var ticksElapsed: Long by alias(internalData::ticksCounter)

    val content: GameContentStore
        get() = gameInstance.content as GameContentStore

    override var sky = World.Sky()

    override val entities
        get() = entities_.asSequence()

    override val decalsManager: DecalsManager = NoOpDecalsManager()
    override val particlesManager: ParticlesManager = NoOpParticlesManager()
    override val soundManager: SoundManager = NoOpSoundManager()

    init {
        try {
            this.generator = gameInstance.content.generators.getWorldGenerator(properties.generator).createForWorld(this)
        } catch (e: Exception) {
            throw WorldLoadingException("Couldn't load world ", e)
        }
    }

    override fun addEntity(entity: Entity): EntityID {
        if (entity.world != this)
            throw Exception("This entity was not created for this world")

        val existingEntity = this.getEntity(entity.id)
        if (existingEntity != null) {
            logger.warn("Tried to add an entity twice (duplicated id ${entity.id}), new entity $entity conflits with $existingEntity")
            return -1
        }

        entities_.add(entity)
        return entity.id
    }

    override fun removeEntity(id: EntityID): Boolean {
        try {
            entitiesLock.writeLock().lock()
            val entity = getEntity(id) ?: return false
            entity.traitLocation.entity.subscribers.toList().forEach { subscriber ->
                subscriber.pushPacket(PacketEntity.createKillerPacket(entity.traitLocation.entity))
            }
            return entities_.remove(entity)
        } finally {
            entitiesLock.writeLock().unlock()
        }
    }

    open fun tick() {

        ticksElapsed++
        gameInstance.pluginManager.fireEvent(WorldTickEvent(this))

        entitiesLock.writeLock().withLock {
            for (entity in entities_) {
                entity.tick()
            }
        }

        // TODO probably belongs in game content
        // Time cycle & weather change
        /*if (this is WorldMaster) internalDataLock.withLock {
            if(internalData.dayNightCycleDuration != 0.0) {
                val increment = (1.0 / TPS) / internalData.dayNightCycleDuration
                internalData.sky.timeOfDay += increment.toFloat()
            }

            if (internalData.varyWeather) {
                val randomFuzz1: Float = (rnd_uniformf() - 0.5f) * 0.0005f * rnd_uniformf()
                val randomFuzz2: Float = (rnd_uniformf() - 0.5f) * 0.0005f * rnd_uniformf()

                internalData.sky.overcast = MathUtils.clampf(internalData.sky.overcast + randomFuzz1, 0.0f, 1.0f)
                internalData.sky.raining = MathUtils.clampf(internalData.sky.raining + randomFuzz2, 0.0f, 1.0f)
            }
        }*/
    }

    override fun getEntitiesInBox(box: Box): Sequence<Entity> {
        return entities.asSequence().filter { it.getBoundingBox().collidesWith(box) }
    }

    override fun getEntity(id: EntityID): Entity? {
        return entities_.find { it.id == id }
    }

    override fun getCell(x: Int, y: Int, z: Int) = getCellMut(x, y, z)
    override fun getCellMut(x: Int, y: Int, z: Int): MutableWorldCell? {
        val x = sanitizeHorizontalCoordinate(x)
        val y = sanitizeVerticalCoordinate(y)
        val z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        return chunk?.getCellMut(x, y, z)
    }

    override fun setCellData(x: Int, y: Int, z: Int, data: CellData): Boolean {
        getCell(x, y, z)?.let { it.data = data } ?: return false
        return true
    }

    override fun pastePrefab(x: Int, y: Int, z: Int, prefab: Prefab): Boolean {
        // println("paste prefab not implemented")
        return false
        // TODO("Not yet implemented")
    }

    /*override fun peekRaw(x: Int, y: Int, z: Int): Int {
        val x = sanitizeHorizontalCoordinate(x)
        val y = sanitizeVerticalCoordinate(y)
        val z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        return chunk?.peekRaw(x, y, z) ?: 0x00000000
    }

    override fun pokeRaw(x: Int, y: Int, z: Int, data: Int) {
        val x = sanitizeHorizontalCoordinate(x)
        val y = sanitizeVerticalCoordinate(y)
        val z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        chunk?.pokeRaw(x, y, z, data)
    }*/

    override fun getCellsInBox(box: Box): Sequence<Cell> {
        val minx = floor(box.min.x)
        val miny = floor(box.min.y)
        val minz = floor(box.min.z)

        val maxx = ceil(box.max.x)
        val maxy = ceil(box.max.y)
        val maxz = ceil(box.max.z)

        return (minx..maxx).asSequence().flatMap { x ->
            (miny..maxy).asSequence().flatMap { y ->
                (minz..maxz).asSequence().mapNotNull { z -> getCell(x, y, z) }
            }
        }
    }

    open fun destroy() {
        ioThread.terminate()
    }

    companion object {
        val worldPropertiesFilename = "properties.json"
    }

    override val logger = LoggerFactory.getLogger("world")
}

open class WorldMasterImplementation constructor(
        gameInstance: GameInstance,
        properties: World.Properties,
        internalData: WorldInternalData,
        contentTranslator: AbstractContentTranslator,
        val folder: File
        ): WorldImplementation(gameInstance, properties, internalData, contentTranslator), WorldMaster {
    override val gameInstance: Host
    get() = super.gameInstance as Host

    val playersMetadata = WorldPlayersMetadata(this)

    override val folderPath: String
        get() = folder.path

    override fun BlockAdditionalData.pushChanges() {
        TODO("Not yet implemented")
    }

    override fun Player.startPlayingAs(entity: Entity) {
        entity.controller = this
        state = PlayerState.Ingame(entity)
    }

    override fun Player.startSpectating() {
        TODO("Not yet implemented")
    }

    override fun Player.pushPacket(packet: PacketWorld) {
        TODO("Not yet implemented")
    }

    override val players: Sequence<Player>
        get() = gameInstance.players

    fun getPlayerByName(playerName: String): Player? {
        return gameInstance.getPlayer(playerName)
    }

    override fun tick() {
        // TODO: processIncommingPackets();
        // TODO: flush all
        super.tick()

        if (ticksElapsed % 60 == 0L) {
            for (region in this.regionsManager.regionsList) {
                // TODO probably should be some sort of async task eventually
                region.compressChangedChunks()
            }
        }

        val physicsRate = 4
        val players = this.players
        if (this.ticksElapsed % physicsRate == 0L) {
            for (chunk in this.chunksManager.allLoadedChunks) {
                var minDistance = Double.MAX_VALUE
                val chunkLocation = Location(this, chunk.chunkX * 32.0 + 16.0, chunk.chunkY * 32.0 + 16.0, chunk.chunkZ * 32.0 + 16.0)
                for(player in players) {
                    val playerLocation = player.entityIfIngame?.location ?: continue
                    val distance = playerLocation.distance(chunkLocation)
                    if(distance < minDistance)
                        minDistance = distance
                }

                if(minDistance < 32 * 2.0) {
                    chunk.tick(this.ticksElapsed / physicsRate)
                }
            }
        }
    }

    override fun addEntity(entity: Entity): EntityID {
        if (entity.id == -1L) {
            val nextUUID = internalDataLock.withLock { internalData.nextEntityId++ }
            entity.id = nextUUID
        }
        return super.addEntity(entity)
    }

    fun saveEverything(): Fence {
        val fence = CompoundFence()
        logger.info("Saving all parts of world " + properties.name)
        fence.add(regionsManager.saveAll())
        saveInternalData()
        return fence
    }

    override fun destroy() {
        super.destroy()
        saveInternalData()
    }
}

fun loadWorld(gameInstance: GameInstance, folder: File): WorldMasterImplementation {
    if (!folder.exists() || !folder.isDirectory)
        throw WorldLoadingException("The folder $folder doesn't exist !")

    val contentTranslatorFile = File(folder.path + "/content_mappings.dat")
    val contentTranslator: AbstractContentTranslator
    if (contentTranslatorFile.exists()) {
        contentTranslator = LoadedContentTranslator.loadFromFile(gameInstance.content as GameContentStore, contentTranslatorFile)
    } else {
        contentTranslator = InitialContentTranslator(gameInstance.content as GameContentStore)
        contentTranslator.save(File(folder.path + "/content_mappings.dat"))
    }

    val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldPropertiesFilename)
    if (!worldInfoFile.exists())
        throw WorldLoadingException("The folder $folder doesn't contain a ${WorldImplementation.worldPropertiesFilename} file !")

    val properties = deserializeWorldInfo(worldInfoFile)
    val internalData = tryLoadWorldInternalData(folder)

    val world = WorldMasterImplementation(gameInstance, properties, internalData, contentTranslator, folder)
    world.ioThread.start()
    return world
}

fun initializeWorld(folder: File, properties: World.Properties) {
    folder.mkdirs()
    val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldPropertiesFilename)
    worldInfoFile.writeText(serializeWorldInfo(properties, true))

    val internalData = WorldInternalData()
    val seedAsByteArray = (properties.seed + "_spawn").toByteArray()
    var i = 0
    var processedSeed: Long = 0
    while (i < 512) {
        for (j in 0..7)
            processedSeed = processedSeed xor ((seedAsByteArray[(i * 3 + j) % seedAsByteArray.size].toLong().shl(j * 8)))
        i += 8
    }

    val random = Random(processedSeed)
    val randomWeather = random.nextFloat()
    internalData.sky = internalData.sky.copy(overcast = randomWeather * randomWeather) // bias towards sunny

    /*val spawnCoordinateX = random.nextInt(properties.size.sizeInChunks * 32)
    val spawnCoordinateZ = random.nextInt(properties.size.sizeInChunks * 32)
    properties.spawn.set(spawnCoordinateX + 0.5, 0.0, spawnCoordinateZ + 0.5)*/

    val internalDataFile = File(folder.path + "/" + worldInternalDataFilename)
    val gson = Gson()
    val contents = gson.toJson(internalData)
    internalDataFile.writeText(contents)
}

class WorldSubImplementation constructor(gameInstance: GameInstance, properties: World.Properties, internalData: WorldInternalData, contentTranslator: AbstractContentTranslator)
    : WorldImplementation(gameInstance, properties, internalData, contentTranslator), WorldSub {
    override val remoteServer: Subscriber
        get() = TODO("Not yet implemented")

    override fun pushPacket(packet: Packet) {
        TODO("Not yet implemented")
    }

    val connection: Connection = TODO()

    fun queueDatagram(d: LogicalPacketDatagram) {
        TODO()
    }
}

//TODO move to worldsize
internal inline fun World.sanitizeHorizontalCoordinate(coordinate: Int): Int {
    var coordinate = coordinate
    coordinate %= (properties.size.squareSizeInBlocks)
    if (coordinate < 0)
        coordinate += properties.size.squareSizeInBlocks
    return coordinate
}

internal inline fun World.sanitizeVerticalCoordinate(coordinate: Int): Int {
    var coordinate = coordinate
    if (coordinate < 0)
        coordinate = 0
    if (coordinate >= properties.size.squareSizeInBlocks)
        coordinate = properties.size.squareSizeInBlocks - 1
    return coordinate
}