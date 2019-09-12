//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

@file:Suppress("NAME_SHADOWING")

package xyz.chunkstories.world

import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock
import org.joml.Vector3dc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitName
import xyz.chunkstories.api.events.player.PlayerSpawnEvent
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.exceptions.world.ChunkNotLoadedException
import xyz.chunkstories.api.exceptions.world.RegionNotLoadedException
import xyz.chunkstories.api.exceptions.world.WorldException
import xyz.chunkstories.api.math.Math2
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.util.IterableIterator
import xyz.chunkstories.api.util.concurrency.Fence
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.*
import xyz.chunkstories.api.world.cell.AbstractCell
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.FutureCell
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.content.sandbox.UnthrustedUserContentSecurityManager
import xyz.chunkstories.content.translator.AbstractContentTranslator
import xyz.chunkstories.content.translator.IncompatibleContentException
import xyz.chunkstories.content.translator.InitialContentTranslator
import xyz.chunkstories.content.translator.LoadedContentTranslator
import xyz.chunkstories.entity.EntityWorldIterator
import xyz.chunkstories.entity.EntityFileSerialization
import xyz.chunkstories.util.alias
import xyz.chunkstories.util.concurrency.CompoundFence
import xyz.chunkstories.world.chunk.ChunksStorage
import xyz.chunkstories.world.heightmap.HeightmapsStorage
import xyz.chunkstories.world.io.IOTasks
import xyz.chunkstories.world.iterators.BlocksInBoundingBoxIterator
import xyz.chunkstories.world.logic.WorldLogicThread
import xyz.chunkstories.world.region.RegionsStorage
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

abstract class WorldImplementation @Throws(WorldLoadingException::class)
constructor(override val gameContext: GameContext, final override val worldInfo: WorldInfo, initialContentTranslator: AbstractContentTranslator?, val folderFile: File?) : World {
    final override val contentTranslator: AbstractContentTranslator

    final override val generator: WorldGenerator

    abstract val ioHandler: IOTasks
    final override val gameLogic: WorldLogicThread

    //TODO delete
    override val maxHeight: Int
        get() = worldInfo.size.heightInChunks * 32
    override val sizeInChunks: Int = worldInfo.size.sizeInChunks
    override val worldSize: Double = (worldInfo.size.sizeInChunks * 32).toDouble()

    final override val chunksManager: ChunksStorage
    final override val regionsManager: RegionsStorage
    final override val heightmapsManager: HeightmapsStorage

    //TODO store the entities in a smarter way ?
    protected val entities: WorldEntitiesHolder
    //TODO go through the code & change write locks into update locks where possible
    var entitiesLock: ReadWriteLock = ReentrantReadWriteUpdateLock()

    final override val collisionsManager: WorldCollisionsManager

    private val internalData: WorldInternalData =
            if (this is WorldMaster) loadInternalDataFromDisk(File(folderPath + "/" + Companion.worldInternalDataFilename))
            else WorldInternalData()

    // The world age, also tick counter. Can count for billions of real-world
    // time so we are not in trouble.
    // Let's say that the game world runs at 60Ticks per second
    final override var ticksElapsed: Long by alias(internalData::ticksCounter)

    // Timecycle counter
    final override var sunCycle: Int by alias(internalData::sunCycleTime)
    final override var weather: Float by alias(internalData::weather)

    override var defaultSpawnLocation: Location
        get() = Location(this, internalData.spawnLocation)
        set(value) {
            internalData.spawnLocation.set(value)
        }

    val folderPath: String
        get() = folderFile?.absolutePath ?: throw Exception("This is not a WorldMaster !")

    override val allLoadedEntities: IterableIterator<Entity>
        get() = EntityWorldIterator(entities.iterator())

    /*override val allLoadedChunks: Sequence<CubicChunk>
        get() = regionsManager.regionsList.asSequence().flatMap { it.loadedChunks.asSequence() }

    override val allLoadedRegions: Collection<Region>
        get() = regionsManager.regionsList*/

    final override val content: Content
        get() = gameContext.content

    init {
        try {
            // Create holders for the world data
            this.chunksManager = ChunksStorage(this)
            this.regionsManager = RegionsStorage(this)
            this.heightmapsManager = HeightmapsStorage(this)

            // And for the citizens
            this.entities = WorldEntitiesHolder(this)

            if (this is WorldMaster) {

                // Check for an existing content translator
                val contentTranslatorFile = File(folderFile!!.path + "/content_mappings.dat")
                if (contentTranslatorFile.exists()) {
                    contentTranslator = LoadedContentTranslator.loadFromFile(gameContext.content,
                            contentTranslatorFile)
                } else {
                    // Build a new content translator
                    contentTranslator = InitialContentTranslator(gameContext.content)
                }

                this.contentTranslator.save(File(this.folderPath + "/content_mappings.dat"))
            } else {
                // Slave world initialization
                if (initialContentTranslator == null) {
                    throw WorldLoadingException("No ContentTranslator providen and none could be found on disk since this is a Slave World.")
                } else {
                    this.contentTranslator = initialContentTranslator
                }
            }

            this.generator = gameContext.content.generators.getWorldGenerator(worldInfo.generatorName).createForWorld(this)
            this.collisionsManager = DefaultWorldCollisionsManager(this)

            // Start the world logic thread
            this.gameLogic = WorldLogicThread(this, UnthrustedUserContentSecurityManager())
        } catch (e: IOException) {
            throw WorldLoadingException("Couldn't load world ", e)
        } catch (e: IncompatibleContentException) {
            throw WorldLoadingException("Couldn't load world ", e)
        }
    }

    fun startLogic() {
        gameLogic.start()
    }

    fun stopLogic(): Fence {
        return gameLogic.stopLogicThread()
    }

    open fun spawnPlayer(player: Player) {
        if (this !is WorldMaster)
            throw UnsupportedOperationException("Only Master Worlds can do this")

        val playerEntityFile = File(this.folderPath + "/players/" + player.name.toLowerCase() + ".json")
        val savedEntity: Entity? = EntityFileSerialization.readEntityFromDisk(playerEntityFile, this)

        var previousLocation: Location? = null
        if (savedEntity != null)
            previousLocation = savedEntity.location

        val playerSpawnEvent = PlayerSpawnEvent(player, this as WorldMaster, savedEntity,
                previousLocation)
        gameContext.pluginManager.fireEvent(playerSpawnEvent)

        if (!playerSpawnEvent.isCancelled) {
            var entity = playerSpawnEvent.entity

            var actualSpawnLocation = playerSpawnEvent.spawnLocation
            if (actualSpawnLocation == null)
                actualSpawnLocation = this.defaultSpawnLocation

            if (entity == null || entity.traits[TraitHealth::class.java]?.isDead == true)
                entity = this.gameContext.content.entities.getEntityDefinition("player")!!
                        .newEntity(this)

            // Name your player !
            entity.traits[TraitName::class.java]?.name = player.name
            entity.traitLocation.set(actualSpawnLocation)
            addEntity(entity)
            player.controlledEntity = entity

            playerEntityFile.delete()
        }
    }

    override fun addEntity(entity: Entity) {
        if (entity.world != this)
            throw Exception("This entity was not created for this world")

        // Assign an UUID to entities lacking one
        if (this is WorldMaster && entity.UUID == -1L) {
            val nextUUID = nextEntityId()
            entity.UUID = nextUUID
        }

        val check = this.getEntityByUUID(entity.UUID)
        if (check != null)
            throw java.lang.Exception("Added an entity twice " + check + " conflits with " + entity + " UUID: " + entity.UUID)

        // Add it to the world
        entity.traitLocation.onSpawn()

        this.entities.insertEntity(entity)
    }

    override fun removeEntity(entity: Entity): Boolean {
        try {
            entitiesLock.writeLock().lock()
            entity.traitLocation.onRemoval()

            // Actually removes it from the world list
            removeEntityFromList(entity)

            return true
        } finally {
            entitiesLock.writeLock().unlock()
        }
    }

    override fun removeEntityByUUID(uuid: Long): Boolean {
        val entityFound = this.getEntityByUUID(uuid)

        return if (entityFound != null)
            removeEntity(entityFound) else false
    }

    /**
     * Internal methods that actually removes the entity from the list after having
     * removed it's reference from elsewere.
     *
     * @return
     */
    fun removeEntityFromList(entity: Entity): Boolean {
        // Remove the entity from the world first
        return entities.removeEntity(entity)
    }

    open fun tick() {
        // Iterates over every entity
        try {
            entitiesLock.writeLock().lock()
            val iter = this.allLoadedEntities
            var entity: Entity
            while (iter.hasNext()) {
                entity = iter.next()
                entity.tick()
            }
        } finally {
            entitiesLock.writeLock().unlock()
        }

        // Increase the ticks counter
        ticksElapsed++

        // Time cycle & weather change
        if (this is WorldMaster && ticksElapsed % 3L == 0L) {
            val increment = internalData.dayNightCycleSpeed
            sunCycle = (sunCycle + increment) % 24000

            if (internalData.varyWeather) {
                val diff = (Math.random() - 0.5f) * 0.0005 * Math.random()
                val rslt = Math2.clamp(internalData.weather + diff, 0.0, 1.0)
                internalData.weather = rslt
            }
        }
    }

    override fun getEntitiesInBox(box: Box): World.NearEntitiesIterator {
        return entities.getEntitiesInBox(box)
    }

    override fun getEntityByUUID(entityID: Long): Entity? {
        return entities.getEntityByUUID(entityID)
    }

    @Throws(WorldException::class)
    override fun tryPeek(location: Vector3dc): ChunkCell {
        return tryPeek(location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    /** Fancy getter method that throws exceptions when the world isn't loaded  */
    @Throws(WorldException::class)
    override fun tryPeek(x: Int, y: Int, z: Int): ChunkCell {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val region = regionsManager.getRegionWorldCoordinates(x, y, z)
                ?: throw RegionNotLoadedException(this, x / 256, y / 256, z / 256)

        val chunk = region.getChunk(x / 32 % 8, y / 32 % 8, z / 32 % 8)
                ?: throw ChunkNotLoadedException(this, region, x / 32 % 8, y / 32 % 8, z / 32 % 8)

        return chunk.peek(x, y, z)
    }

    override fun peek(x: Int, y: Int, z: Int): WorldCell {
        var x = x
        var y = y
        var z = z

        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val region = regionsManager.getRegionWorldCoordinates(x, y, z)
                ?: return UnloadedWorldCell(x, y, z, this.gameContext.content.voxels.air, 0, 0, 0)

        val chunk = region.getChunk(x / 32 % 8, y / 32 % 8, z / 32 % 8)
                ?: return UnloadedWorldCell(x, y, z, this.gameContext.content.voxels.air, 0, 0, 0)

        return chunk.peek(x, y, z)
    }

    /** Safety: provide an alternative 'fake' getCell if the proper one isn't loaded  */
    internal inner class UnloadedWorldCell(x: Int, y: Int, z: Int, voxel: Voxel, meta: Int, blocklight: Int, sunlight: Int) : AbstractCell(x, y, z, voxel, meta, blocklight, sunlight), WorldCell {
        override val world: World
            get() = this@WorldImplementation

        init {

            val groundHeight = heightmapsManager.getHeightAtWorldCoordinates(x, z)
            if (groundHeight < y && groundHeight != Heightmap.NO_DATA)
                this.sunlight = 15
        }

        override fun getNeightbor(side_int: Int): Cell {
            val side = VoxelSide.values()[side_int]
            return peek(x + side.dx, y + side.dy, z + side.dz)
        }

        /*override fun setVoxel(voxel: Voxel) {
            logger.warn("Trying to edit a UnloadedWorldCell." + this)
        }

        override fun setMetaData(metadata: Int) {
            logger.warn("Trying to edit a UnloadedWorldCell." + this)
        }

        override fun setSunlight(sunlight: Int) {
            logger.warn("Trying to edit a UnloadedWorldCell." + this)
        }

        override fun setBlocklight(blocklight: Int) {
            logger.warn("Trying to edit a UnloadedWorldCell." + this)
        }*/
    }

    override fun peek(location: Vector3dc): WorldCell {
        return peek(location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    override fun peekSimple(x: Int, y: Int, z: Int): Voxel {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        return if (chunk == null)
            gameContext.content.voxels.air
        else
            chunk.peekSimple(x, y, z)
    }

    override fun peekRaw(x: Int, y: Int, z: Int): Int {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        return chunk?.peekRaw(x, y, z) ?: 0x00000000
    }

    @Throws(WorldException::class)
    override fun poke(x: Int, y: Int, z: Int, voxel: Voxel?, sunlight: Int, blocklight: Int, metadata: Int,
                      cause: WorldModificationCause?): ChunkCell {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val region = regionsManager.getRegionWorldCoordinates(x, y, z)
                ?: throw RegionNotLoadedException(this, x / 256, y / 256, z / 256)

        val chunk = region.getChunk(x / 32 % 8, y / 32 % 8, z / 32 % 8)
                ?: throw ChunkNotLoadedException(this, region, x / 32 % 8, y / 32 % 8, z / 32 % 8)

        return chunk.poke(x, y, z, voxel, sunlight, blocklight, metadata, cause)
    }

    @Throws(WorldException::class)
    override fun poke(future: FutureCell, cause: WorldModificationCause?): ChunkCell {
        return poke(future.x, future.y, future.z, future.voxel, future.sunlight,
                future.blocklight, future.metaData, cause)
    }

    override fun pokeSimple(x: Int, y: Int, z: Int, voxel: Voxel?, sunlight: Int, blocklight: Int, metadata: Int) {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        chunk?.pokeSimple(x, y, z, voxel, sunlight, blocklight, metadata)
    }

    override fun pokeSimple(future: FutureCell) {
        pokeSimple(future.x, future.y, future.z, future.voxel, future.sunlight,
                future.blocklight, future.metaData)
    }

    override fun pokeSimpleSilently(x: Int, y: Int, z: Int, voxel: Voxel?, sunlight: Int, blocklight: Int, metadata: Int) {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        chunk?.pokeSimpleSilently(x, y, z, voxel, sunlight, blocklight, metadata)
    }

    override fun pokeSimpleSilently(future: FutureCell) {
        pokeSimpleSilently(future.x, future.y, future.z, future.voxel, future.sunlight,
                future.blocklight, future.metaData)
    }

    override fun pokeRaw(x: Int, y: Int, z: Int, raw_data: Int) {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        chunk?.pokeRaw(x, y, z, raw_data)
    }

    override fun pokeRawSilently(x: Int, y: Int, z: Int, raw_data: Int) {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = chunksManager.getChunkWorldCoordinates(x, y, z)
        chunk?.pokeRawSilently(x, y, z, raw_data)
    }

    override fun getVoxelsWithin(boundingBox: Box): IterableIterator<Cell> {
        return BlocksInBoundingBoxIterator(this, boundingBox)
    }

    /** Requests a full serialization of the world  */
    fun saveEverything(): Fence {
        val ioOperationsFence = CompoundFence()

        logger.info("Saving all parts of world " + worldInfo.name)
        ioOperationsFence.add(regionsManager.saveAll())
        //ioOperationsFence.add(regionsSummariesHolder.saveAllLoadedSummaries())

        saveInternalData()

        return ioOperationsFence
    }

    val entityUUIDLock = ReentrantLock()
    fun nextEntityId(): Long {
        entityUUIDLock.withLock {
            return internalData.nextEntityId++
        }
    }

    //TODO move to worldsize
    internal fun sanitizeHorizontalCoordinate(coordinate: Int): Int {
        var coordinate = coordinate
        coordinate %= (sizeInChunks * 32)
        if (coordinate < 0)
            coordinate += sizeInChunks * 32
        return coordinate
    }

    //TODO move to worldsize
    internal fun sanitizeVerticalCoordinate(coordinate: Int): Int {
        var coordinate = coordinate
        if (coordinate < 0)
            coordinate = 0
        if (coordinate >= worldInfo.size.heightInChunks * 32)
            coordinate = worldInfo.size.heightInChunks * 32 - 1
        return coordinate
    }


    open fun destroy() {
        // Stop the game logic first
        gameLogic.stopLogicThread().traverse()

        //this.regionsStorage!!.destroy()
        //this.regionsSummariesHolder.destroy()

        // Always, ALWAYS save this.
        if (this is WorldMaster) {
            saveInternalData()
        }

        // Kill the IO handler
        ioHandler.kill()
    }

    private fun saveInternalData() {
        internalData.writeToDisk(File(this.folderPath + "/" + worldInternalDataFilename))
    }

    fun logger(): Logger {
        return logger
    }

    companion object {
        private val logger = LoggerFactory.getLogger("world")

        val worldInfoFilename = "worldInfo.json"
        val worldInternalDataFilename = "internalData.json"
    }
}
