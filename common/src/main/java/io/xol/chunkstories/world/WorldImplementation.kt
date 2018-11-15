//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world

import io.xol.chunkstories.api.GameContext
import io.xol.chunkstories.api.GameLogic
import io.xol.chunkstories.api.Location
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.content.ContentTranslator
import io.xol.chunkstories.api.entity.Entity
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth
import io.xol.chunkstories.api.entity.traits.serializable.TraitName
import io.xol.chunkstories.api.events.player.PlayerSpawnEvent
import io.xol.chunkstories.api.events.voxel.WorldModificationCause
import io.xol.chunkstories.api.exceptions.world.ChunkNotLoadedException
import io.xol.chunkstories.api.exceptions.world.RegionNotLoadedException
import io.xol.chunkstories.api.exceptions.world.WorldException
import io.xol.chunkstories.api.input.Input
import io.xol.chunkstories.api.physics.Box
import io.xol.chunkstories.api.player.Player
import io.xol.chunkstories.api.util.IterableIterator
import io.xol.chunkstories.api.util.concurrency.Fence
import io.xol.chunkstories.api.voxel.Voxel
import io.xol.chunkstories.api.voxel.VoxelSide
import io.xol.chunkstories.api.world.*
import io.xol.chunkstories.api.world.cell.Cell
import io.xol.chunkstories.api.world.cell.CellData
import io.xol.chunkstories.api.world.cell.FutureCell
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkCell
import io.xol.chunkstories.api.world.chunk.ChunkHolder
import io.xol.chunkstories.api.world.chunk.ChunksIterator
import io.xol.chunkstories.api.world.generator.WorldGenerator
import io.xol.chunkstories.api.world.heightmap.Heightmap
import io.xol.chunkstories.content.sandbox.UnthrustedUserContentSecurityManager
import io.xol.chunkstories.content.translator.AbstractContentTranslator
import io.xol.chunkstories.content.translator.IncompatibleContentException
import io.xol.chunkstories.content.translator.InitialContentTranslator
import io.xol.chunkstories.content.translator.LoadedContentTranslator
import io.xol.chunkstories.entity.EntityWorldIterator
import io.xol.chunkstories.entity.SerializedEntityFile
import io.xol.chunkstories.util.concurrency.CompoundFence
import io.xol.chunkstories.world.chunk.CubicChunk
import io.xol.chunkstories.world.heightmap.WorldHeightmapsImplementation
import io.xol.chunkstories.world.io.IOTasks
import io.xol.chunkstories.world.iterators.AABBVoxelIterator
import io.xol.chunkstories.world.iterators.WorldChunksIterator
import io.xol.chunkstories.world.logic.WorldLogicThread
import io.xol.chunkstories.world.region.HashMapWorldRegionsHolder
import io.xol.chunkstories.world.region.RegionImplementation
import org.joml.Vector3dc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

abstract class WorldImplementation @Throws(WorldLoadingException::class)
constructor(override val gameContext: GameContext, info: WorldInfo, initialContentTranslator: AbstractContentTranslator?, folder: File?) : World {
    //override val gameContext: GameContext

    final override val worldInfo: WorldInfo
    final override val contentTranslator: AbstractContentTranslator

    val folderFile: File?

    private val internalData = Properties()
    private val internalDataFile: File?

    final override val generator: WorldGenerator

    // The world age, also tick counter. Can count for billions of real-world
    // time so we are not in trouble.
    // Let's say that the game world runs at 60Ticks per second
    override var ticksElapsed: Long = 0

    // Timecycle counter
    override var time: Long = 5000
    override var weather = 0.2f

    // Who does the actual work
    abstract val ioHandler: IOTasks
    private val worldThread: WorldLogicThread

    //Data holding
    val regionsHolder: HashMapWorldRegionsHolder
    final override val regionsSummariesHolder: WorldHeightmapsImplementation

    // Temporary entity list
    protected val entities: WorldEntitiesHolder

    var entitiesLock: ReadWriteLock = ReentrantReadWriteLock(true)

    final override val collisionsManager: WorldCollisionsManager

    // Entity IDS counter
    internal var entitiesUUIDGenerator = AtomicLong()

    val folderPath: String
        get() = folderFile?.absolutePath ?: throw Exception("This is not a WorldMaster !")

    override val allLoadedEntities: IterableIterator<Entity>
        get() = EntityWorldIterator(entities.iterator())
    override val maxHeight: Int
        get() = worldInfo.size.heightInChunks * 32

    override val sizeInChunks: Int
        get() = worldInfo.size.sizeInChunks

    override val worldSize: Double
        get() = (worldInfo.size.sizeInChunks * 32).toDouble()

    override var defaultSpawnLocation: Location
        get() {
            val dx = internalData.getProperty("defaultSpawnX")?.toDoubleOrNull() ?: worldSize * 0.5
            val dy = internalData.getProperty("defaultSpawnY")?.toDoubleOrNull() ?: 100.0
            val dz = internalData.getProperty("defaultSpawnZ")?.toDoubleOrNull() ?: worldSize * 0.5
            return Location(this, dx, dy, dz)
        }
        set(location) {
            internalData.setProperty("defaultSpawnX", "${location.x()}")
            internalData.setProperty("defaultSpawnY", "${location.y()}")
            internalData.setProperty("defaultSpawnZ", "${location.z()}")
        }

    override val allLoadedChunks: Collection<CubicChunk>
        get() = regionsHolder.internalGetLoadedChunks()

    final override val gameLogic: GameLogic
        get() = worldThread

    final override val content: Content
        get() = gameContext.content

    init {
        try {
            //this.gameContext = gameContext
            this.worldInfo = info

            // Create holders for the world data
            this.regionsHolder = HashMapWorldRegionsHolder(this)
            this.regionsSummariesHolder = WorldHeightmapsImplementation(this)

            // And for the citizens
            this.entities = WorldEntitiesHolder(this)

            if (this is WorldMaster) {
                // Obtain the parent folder
                this.folderFile = folder

                // Check for an existing content translator
                val contentTranslatorFile = File(folder!!.path + "/content_mappings.dat")
                if (contentTranslatorFile.exists()) {
                    contentTranslator = LoadedContentTranslator.loadFromFile(gameContext.content,
                            contentTranslatorFile)
                } else {
                    // Build a new content translator
                    contentTranslator = InitialContentTranslator(gameContext.content)
                }

                this.contentTranslator.save(File(this.folderPath!! + "/content_mappings.dat"))

                internalDataFile = File(folder.path + "/internal.dat")
                if(internalDataFile.exists())
                    this.internalData.load(FileReader(internalDataFile))

                this.entitiesUUIDGenerator.set(internalData.getProperty("entities-ids-counter", "0").toLong())
                this.time = internalData.getProperty("worldTime")?.toLongOrNull() ?: 5000
                this.ticksElapsed = internalData.getProperty("worldTimeInternal")?.toLongOrNull() ?: 0
                this.weather = internalData.getProperty("overcastFactor")?.toFloatOrNull() ?: 0.2F
            } else {
                // Slave world initialization
                if (initialContentTranslator == null) {
                    throw WorldLoadingException("No ContentTranslator providen and none could be found on disk since this is a Slave World.")
                } else {
                    this.contentTranslator = initialContentTranslator
                }

                // Null-out final fields meant for master worlds
                this.folderFile = null
                this.internalDataFile = null
            }

            this.generator = gameContext.content.generators().getWorldGenerator(info.generatorName).createForWorld(this)
            this.collisionsManager = DefaultWorldCollisionsManager(this)

            // Start the world logic thread
            this.worldThread = WorldLogicThread(this, UnthrustedUserContentSecurityManager())
        } catch (e: IOException) {
            throw WorldLoadingException("Couldn't load world ", e)
        } catch (e: IncompatibleContentException) {
            throw WorldLoadingException("Couldn't load world ", e)
        }
    }

    fun startLogic() {
        worldThread.start()
    }

    fun stopLogic(): Fence {
        return worldThread.stopLogicThread()
    }

    open fun spawnPlayer(player: Player) {
        if (this !is WorldMaster)
            throw UnsupportedOperationException("Only Master Worlds can do this")

        var savedEntity: Entity? = null

        val playerEntityFile = SerializedEntityFile(
                this.folderPath + "/players/" + player.name.toLowerCase() + ".csf")
        if (playerEntityFile.exists())
            savedEntity = playerEntityFile.read(this)

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
                entity = this.gameContext.content.entities().getEntityDefinition("player")!!
                        .newEntity(this)
            //else
            //    entity.UUID = -1

            // Name your player !
            entity.traits[TraitName::class.java]?.name = player.name
            entity.traitLocation.set(actualSpawnLocation)
            addEntity(entity)
            player.controlledEntity = entity
        }
    }

    override fun addEntity(entity: Entity) {
        // Assign an UUID to entities lacking one
        if (this is WorldMaster && entity.UUID == -1L) {
            val nextUUID = nextEntityId()
            entity.UUID = nextUUID
        }

        val check = this.getEntityByUUID(entity.UUID)
        if (check != null) {
            logger().error(
                    "Added an entity twice " + check + " conflits with " + entity + " UUID: " + entity.UUID)
            // logger().save();
            Thread.dumpStack()
            return // System.exit(-1);
        }

        // Add it to the world
        entity.traitLocation.onSpawn()

        assert(entity.getWorld() === this)

        /*
		 * Chunk chunk = this.getChunkWorldCoordinates(entity.getLocation()); if(chunk
		 * != null) { ((EntityBase)entity).positionComponent.trySnappingToChunk(); }
		 */

        this.entities.insertEntity(entity)
    }

    override fun removeEntity(entity: Entity): Boolean {
        try {
            entitiesLock.writeLock().lock()
            if (entity != null) {
                entity.traitLocation.onRemoval()

                // Actually removes it from the world list
                removeEntityFromList(entity)

                return true
            }

            return false
        } finally {
            entitiesLock.writeLock().unlock()
        }
    }

    override fun removeEntityByUUID(uuid: Long): Boolean {
        val entityFound = this.getEntityByUUID(uuid)

        return if (entityFound != null) removeEntity(entityFound) else false

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

                // Check entity's region is loaded
                // if (entity.TraitLocation.getChunk() != null)
                entity.tick()

                // Tries to snap the entity to the region if it ends up being loaded
                // else
                // ((EntityBase)entity).positionComponent.trySnappingToChunk();

            }
        } finally {
            entitiesLock.writeLock().unlock()
        }

        // Increase the ticks counter
        ticksElapsed++

        // Time cycle
        if (this is WorldMaster && internalData.getProperty("doTimeCycle")?.toBoolean() == true)
            if (ticksElapsed % 60 == 0L)
                time++
    }

    override fun getEntitiesInBox(center: Vector3dc, boxSize: Vector3dc): World.NearEntitiesIterator {
        return entities.getEntitiesInBox(center, boxSize)
    }

    override fun getEntityByUUID(entityID: Long): Entity? {
        return entities.getEntityByUUID(entityID)
    }

    @Throws(WorldException::class)
    override fun peek(location: Vector3dc): ChunkCell {
        return peek(location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    @Throws(WorldException::class)
    override
            /** Fancy getVoxelComponent method that throws exceptions when the world isn't loaded  */
    fun peek(x: Int, y: Int, z: Int): ChunkCell {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val region = this.getRegionWorldCoordinates(x, y, z)
                ?: throw RegionNotLoadedException(this, x / 256, y / 256, z / 256)

        val chunk = region.getChunk(x / 32 % 8, y / 32 % 8, z / 32 % 8)
                ?: throw ChunkNotLoadedException(region, x / 32 % 8, y / 32 % 8, z / 32 % 8)

        return chunk.peek(x, y, z)
    }

    override fun peekSafely(x: Int, y: Int, z: Int): World.WorldCell {
        var x = x
        var y = y
        var z = z

        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val region = this.getRegionWorldCoordinates(x, y, z)
                ?: return UnloadedWorldCell(x, y, z, this.gameContext.content.voxels().air(), 0, 0, 0)

        val chunk = region.getChunk(x / 32 % 8, y / 32 % 8, z / 32 % 8)
                ?: return UnloadedWorldCell(x, y, z, this.gameContext.content.voxels().air(), 0, 0, 0)

        return chunk.peek(x, y, z)
    }

    /** Safety: provide an alternative 'fake' getCell if the proper one isn't loaded  */
    internal inner class UnloadedWorldCell(x: Int, y: Int, z: Int, voxel: Voxel, meta: Int, blocklight: Int, sunlight: Int) : Cell(x, y, z, voxel, meta, blocklight, sunlight), World.WorldCell {

        init {

            val groundHeight = regionsSummariesHolder.getHeightAtWorldCoordinates(x, z)
            if (groundHeight < y && groundHeight != Heightmap.NO_DATA)
                this.sunlight = 15
        }

        override fun getNeightbor(side_int: Int): CellData {
            val side = VoxelSide.values()[side_int]
            return peekSafely(getX() + side.dx, getY() + side.dy, getZ() + side.dz)
        }

        override fun getWorld(): World {
            return this@WorldImplementation
        }

        override fun setVoxel(voxel: Voxel) {
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
        }
    }

    override fun peekSafely(location: Vector3dc): World.WorldCell {
        return peekSafely(location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    override fun peekSimple(x: Int, y: Int, z: Int): Voxel {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = this.getChunkWorldCoordinates(x, y, z)
        return if (chunk == null)
            gameContext.content.voxels().air()
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

        val chunk = this.getChunkWorldCoordinates(x, y, z)
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

        val region = this.getRegionWorldCoordinates(x, y, z)
                ?: throw RegionNotLoadedException(this, x / 256, y / 256, z / 256)

        val chunk = region.getChunk(x / 32 % 8, y / 32 % 8, z / 32 % 8)
                ?: throw ChunkNotLoadedException(region, x / 32 % 8, y / 32 % 8, z / 32 % 8)

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

        val chunk = this.getChunkWorldCoordinates(x, y, z)
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

        val chunk = this.getChunkWorldCoordinates(x, y, z)
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

        val chunk = this.getChunkWorldCoordinates(x, y, z)
        chunk?.pokeRaw(x, y, z, raw_data)
    }

    override fun pokeRawSilently(x: Int, y: Int, z: Int, raw_data: Int) {
        var x = x
        var y = y
        var z = z
        x = sanitizeHorizontalCoordinate(x)
        y = sanitizeVerticalCoordinate(y)
        z = sanitizeHorizontalCoordinate(z)

        val chunk = this.getChunkWorldCoordinates(x, y, z)
        chunk?.pokeRawSilently(x, y, z, raw_data)
    }

    override fun getVoxelsWithin(boundingBox: Box): IterableIterator<CellData> {
        return AABBVoxelIterator(this, boundingBox)
    }

    /** Requests a full serialization of the world  */
    fun saveEverything(): Fence {
        val ioOperationsFence = CompoundFence()

        logger.info("Saving all parts of world " + worldInfo.name)
        ioOperationsFence.add(regionsHolder.saveAll())
        ioOperationsFence.add(regionsSummariesHolder.saveAllLoadedSummaries())

        this.internalData.setProperty("entities-ids-counter", "" + entitiesUUIDGenerator.get())
        this.internalData.setProperty("worldTime", "" + time)
        this.internalData.setProperty("worldTimeInternal", "" + ticksElapsed)
        this.internalData.setProperty("overcastFactor", "" + weather)
        saveInternalData()

        return ioOperationsFence
    }

    fun nextEntityId(): Long {
        return entitiesUUIDGenerator.getAndIncrement()
    }

    override fun handleInteraction(entity: Entity, voxelLocation: Location?, input: Input): Boolean {
        if (voxelLocation == null)
            return false

        val peek: CellData
        try {
            peek = this.peek(voxelLocation)
        } catch (e: WorldException) {
            // Will not accept interacting with unloaded blocks
            return false
        }

        return peek.getVoxel()!!.handleInteraction(entity, peek, input)
    }

    private fun sanitizeHorizontalCoordinate(coordinate: Int): Int {
        var coordinate = coordinate
        coordinate = coordinate % (sizeInChunks * 32)
        if (coordinate < 0)
            coordinate += sizeInChunks * 32
        return coordinate
    }

    private fun sanitizeVerticalCoordinate(coordinate: Int): Int {
        var coordinate = coordinate
        if (coordinate < 0)
            coordinate = 0
        if (coordinate >= worldInfo.size.heightInChunks * 32)
            coordinate = worldInfo.size.heightInChunks * 32 - 1
        return coordinate
    }

    override fun acquireChunkHolderLocation(user: WorldUser, location: Location): ChunkHolder? {
        return acquireChunkHolder(user, location.x().toInt(), location.y().toInt(),
                location.z().toInt())
    }

    override fun acquireChunkHolder(user: WorldUser, chunkX: Int, chunkY: Int, chunkZ: Int): ChunkHolder? {
        var chunkX = chunkX
        var chunkZ = chunkZ
        // Sanitation of input data
        chunkX = chunkX % sizeInChunks
        chunkZ = chunkZ % sizeInChunks
        if (chunkX < 0)
            chunkX += sizeInChunks
        if (chunkZ < 0)
            chunkZ += sizeInChunks
        return this.regionsHolder!!.acquireChunkHolder(user, chunkX, chunkY, chunkZ)
    }

    override fun acquireChunkHolderWorldCoordinates(user: WorldUser, worldX: Int, worldY: Int, worldZ: Int): ChunkHolder? {
        var worldX = worldX
        var worldY = worldY
        var worldZ = worldZ
        worldX = sanitizeHorizontalCoordinate(worldX)
        worldY = sanitizeVerticalCoordinate(worldY)
        worldZ = sanitizeHorizontalCoordinate(worldZ)

        return this.regionsHolder!!.acquireChunkHolder(user, worldX / 32, worldY / 32, worldZ / 32)
    }

    override fun isChunkLoaded(chunkX: Int, chunkY: Int, chunkZ: Int): Boolean {
        var chunkX = chunkX
        var chunkZ = chunkZ
        // Sanitation of input data
        chunkX = chunkX % sizeInChunks
        chunkZ = chunkZ % sizeInChunks
        if (chunkX < 0)
            chunkX += sizeInChunks
        if (chunkZ < 0)
            chunkZ += sizeInChunks
        // Out of bounds checks
        if (chunkY < 0)
            return false
        return if (chunkY >= worldInfo.size.heightInChunks) false else this.regionsHolder!!.getChunk(chunkX, chunkY, chunkZ) != null
        // If it doesn't return null then it exists
    }

    override fun getChunkWorldCoordinates(location: Location): CubicChunk? {
        return getChunkWorldCoordinates(location.x().toInt(), location.y().toInt(),
                location.z().toInt())
    }

    override fun getChunkWorldCoordinates(worldX: Int, worldY: Int, worldZ: Int): CubicChunk? {
        return getChunk(worldX / 32, worldY / 32, worldZ / 32)
    }

    override fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int): CubicChunk? {
        var chunkX = chunkX
        var chunkZ = chunkZ
        chunkX = chunkX % sizeInChunks
        chunkZ = chunkZ % sizeInChunks
        if (chunkX < 0)
            chunkX += sizeInChunks
        if (chunkZ < 0)
            chunkZ += sizeInChunks
        if (chunkY < 0)
            return null
        return if (chunkY >= worldInfo.size.heightInChunks) null else regionsHolder!!.getChunk(chunkX, chunkY, chunkZ)
    }

    override fun acquireRegion(user: WorldUser, regionX: Int, regionY: Int, regionZ: Int): RegionImplementation? {
        return this.regionsHolder!!.acquireRegion(user, regionX, regionY, regionZ)
    }

    override fun acquireRegionChunkCoordinates(user: WorldUser, chunkX: Int, chunkY: Int, chunkZ: Int): RegionImplementation? {
        return acquireRegion(user, chunkX / 8, chunkY / 8, chunkZ / 8)
    }

    override fun acquireRegionWorldCoordinates(user: WorldUser, worldX: Int, worldY: Int, worldZ: Int): RegionImplementation? {
        var worldX = worldX
        var worldY = worldY
        var worldZ = worldZ
        worldX = sanitizeHorizontalCoordinate(worldX)
        worldY = sanitizeVerticalCoordinate(worldY)
        worldZ = sanitizeHorizontalCoordinate(worldZ)

        return acquireRegion(user, worldX / 256, worldY / 256, worldZ / 256)
    }

    override fun acquireRegionLocation(user: WorldUser, location: Location): RegionImplementation? {
        return acquireRegionWorldCoordinates(user, location.x().toInt(), location.y().toInt(),
                location.z().toInt())
    }

    override fun getRegionLocation(location: Location): RegionImplementation? {
        return getRegionWorldCoordinates(location.x().toInt(), location.y().toInt(),
                location.z().toInt())
    }

    override fun getRegionWorldCoordinates(worldX: Int, worldY: Int, worldZ: Int): RegionImplementation? {
        var worldX = worldX
        var worldY = worldY
        var worldZ = worldZ
        worldX = sanitizeHorizontalCoordinate(worldX)
        worldY = sanitizeVerticalCoordinate(worldY)
        worldZ = sanitizeHorizontalCoordinate(worldZ)

        return getRegion(worldX / 256, worldY / 256, worldZ / 256)
    }

    override fun getRegionChunkCoordinates(chunkX: Int, chunkY: Int, chunkZ: Int): RegionImplementation? {
        return getRegion(chunkX / 8, chunkY / 8, chunkZ / 8)
    }

    override fun getRegion(regionX: Int, regionY: Int, regionZ: Int): RegionImplementation? {
        return regionsHolder!!.getRegion(regionX, regionY, regionZ)
    }

    open fun destroy() {
        // Stop the game logic first
        worldThread!!.stopLogicThread().traverse()

        this.regionsHolder!!.destroy()
        this.regionsSummariesHolder.destroy()

        // Always, ALWAYS save this.
        if (this is WorldMaster) {
            this.internalData.setProperty("entities-ids-counter", "" + entitiesUUIDGenerator.get())
            saveInternalData()
        }

        // Kill the IO handler
        ioHandler!!.kill()
    }

    private fun saveInternalData() {
        this.internalData.store(FileWriter(internalDataFile), "Autogenerated file, avoid modifying")
    }

    fun logger(): Logger {
        return logger
    }

    companion object {

        private val logger = LoggerFactory.getLogger("world")
    }
}
