//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.chunk

import org.joml.Vector3dc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.json.*
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.exceptions.world.WorldException
import xyz.chunkstories.api.net.packets.PacketVoxelUpdate
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelFormat
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.chunk.*
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.api.world.region.Region
import xyz.chunkstories.util.concurrency.SimpleLock
import xyz.chunkstories.voxel.components.CellComponentsHolder
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldTool
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty
import xyz.chunkstories.world.chunk.deriveddata.ChunkOcclusionProperty
import xyz.chunkstories.world.region.RegionImplementation
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Essential class that holds actual chunk voxel data, entities and voxel
 * component !
 */
class ChunkImplementation(override val holder: ChunkHolderImplementation, override val chunkX: Int, override val chunkY: Int, override val chunkZ: Int, compressedData: ChunkCompressedData?) : Chunk {
    override val world: WorldImplementation
    protected val holdingRegion: RegionImplementation
    protected val uuid: Int

    // Actual data holding here
    var voxelDataArray: IntArray? = null

    // Count unsaved edits atomically, fancy :]
    val compressionUncommitedModifications = AtomicInteger()
    val revision = AtomicLong(0)

    override val occlusion: ChunkOcclusionManager
    override val lightBaker: ChunkLightBaker
    override lateinit var mesh: ChunkMesh

    // Set to true after destroy()
    var isDestroyed = false
    val chunkDestructionSemaphore = Semaphore(1)

    val allCellComponents: MutableMap<Int, CellComponentsHolder> = HashMap()
    val localEntities: MutableSet<Entity> = ConcurrentHashMap.newKeySet()

    //TODO use semaphores/RW locks
    val componentsLock = SimpleLock()
    val entitiesLock = SimpleLock()

    private val chunkDataArrayCreation = Semaphore(1)

    override val isAirChunk: Boolean
        get() = voxelDataArray == null

    override val region: Region
        get() = holdingRegion

    override val entitiesWithinChunk: Collection<Entity>
        get() = localEntities

    init {
        //var compressedData = compressedData
        chunksCounter.incrementAndGet()

        this.holdingRegion = holder.region
        this.world = holdingRegion.world

        this.uuid = chunkX shl world.worldInfo.size.bitlengthOfVerticalChunksCoordinates or chunkY shl world
                .worldInfo.size.bitlengthOfHorizontalChunksCoordinates or chunkZ

        occlusion = ChunkOcclusionProperty(this)
        lightBaker = ChunkLightBaker(this)

        if (compressedData != null) {
            if(compressedData is ChunkCompressedData.NonAir) {
                this.voxelDataArray = compressedData.extractVoxelData()

                val extendedData = compressedData.extractVoxelExtendedData()
                for (cellWithExtendedData in extendedData.elements) {
                    val index = (cellWithExtendedData as? Json.Dict ?: continue)["index"].asInt!!

                    val components = CellComponentsHolder(this, index)
                    allCellComponents[index] = components

                    // Call the block's onPlace method as to make it spawn the necessary components
                    val peek = peek(components.x, components.y, components.z)
                    val future = FreshFutureCell(this, peek)
                    peek.voxel.whenPlaced(future)

                    val savedComponents = cellWithExtendedData["components"].asArray!!
                    for (savedComponent in savedComponents.elements) {
                        val dict = savedComponent.asDict ?: continue
                        val name = dict["name"].asString!!
                        val data = dict["data"]!!

                        val component = components.getVoxelComponent(name)
                        if (component == null) {
                            logger.warn("Component named $name was saved, but was not recreated by the voxel whenPlaced() method.")
                            continue
                        }
                        component.deserialize(data)
                    }
                }
            }

            val savedEntities = compressedData.extractEntities()
            for(savedEntity in savedEntities.elements) {
                val entity = EntitySerialization.deserializeEntity(world, savedEntity)
                this.addEntity(entity)
                world.addEntity(entity)
            }
        }

        mesh = DummyChunkRenderingData

        // Send chunk to whoever already subscribed
        //if (compressedData == null)
        //    compressedData = CompressedData(null, null, null)
    }

    private fun sanitizeCoordinate(a: Int): Int {
        return a and 0x1F
    }

    override fun peek(x: Int, y: Int, z: Int): ActualChunkCell {
        return ActualChunkCell(this, x, y, z, peekRaw(x, y, z))
    }

    override fun peek(location: Vector3dc): ActualChunkCell {
        return peek(location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    override fun peekSimple(x: Int, y: Int, z: Int): Voxel {
        return world.contentTranslator.getVoxelForId(VoxelFormat.id(peekRaw(x, y, z))) ?: world.content.voxels.air
    }

    override fun peekRaw(x: Int, y: Int, z: Int): Int {
        var x = x
        var y = y
        var z = z
        x = sanitizeCoordinate(x)
        y = sanitizeCoordinate(y)
        z = sanitizeCoordinate(z)

        if (voxelDataArray == null) {
            // Empty chunk ?
            // Use the heightmap to figure out wether or not that getCell should be skylit.
            var sunlight = 0
            val groundHeight = holdingRegion.heightmap.getHeight(x, z)
            //int groundHeight = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + x, chunkZ * 32 + z);
            if (groundHeight < y + chunkY * 32 && groundHeight != Heightmap.NO_DATA)
                sunlight = 15

            return VoxelFormat.format(0, 0, sunlight, 0)
        } else {
            return voxelDataArray!![x * 32 * 32 + y * 32 + z]
        }
    }

    override fun poke(x: Int, y: Int, z: Int, voxel: Voxel?, sunlight: Int, blocklight: Int, metadata: Int, cause: WorldModificationCause?): ChunkCell {
        return pokeInternal(x, y, z, voxel, sunlight, blocklight, metadata, 0x00, false, true, true, cause)!!
    }

    override fun pokeSimple(x: Int, y: Int, z: Int, voxel: Voxel?, sunlight: Int, blocklight: Int, metadata: Int) {
        pokeInternal(x, y, z, voxel, sunlight, blocklight, metadata, 0x00, false, true, false, null)
    }

    override fun pokeSimpleSilently(x: Int, y: Int, z: Int, voxel: Voxel?, sunlight: Int, blocklight: Int, metadata: Int) {
        pokeInternal(x, y, z, voxel, sunlight, blocklight, metadata, 0x00, false, false, false, null)
    }

    override fun pokeRaw(x: Int, y: Int, z: Int, raw_data_bits: Int) {
        pokeInternal(x, y, z, null, 0, 0, 0, raw_data_bits, true, true, false, null)
    }

    override fun pokeRawSilently(x: Int, y: Int, z: Int, raw_data_bits: Int) {
        pokeInternal(x, y, z, null, 0, 0, 0, raw_data_bits, true, false, false, null)
    }

    /**
     * The 'core' of the core, this private function is responsible for placing and
     * keeping everyone up to snuff on block modifications. It all comes back to
     * this really.
     */
    private fun pokeInternal(worldX: Int, worldY: Int, worldZ: Int, newVoxel: Voxel?,
                             sunlight: Int, blocklight: Int, metadata: Int, raw_data: Int, use_raw_data: Boolean,
                             update: Boolean, return_context: Boolean, cause: WorldModificationCause?): ActualChunkCell? {
        var newVoxel = newVoxel
        var raw_data = raw_data
        val x = sanitizeCoordinate(worldX)
        val y = sanitizeCoordinate(worldY)
        val z = sanitizeCoordinate(worldZ)

        val cell_pre = peek(x, y, z)
        val formerVoxel = cell_pre.voxel

        val future = FreshFutureCell(this, cell_pre)

        if (use_raw_data) {
            // We need this for voxel placement logic
            newVoxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(raw_data))
            // Build the future from parsing the raw data
            newVoxel?.let { future.voxel = it}
            future.sunlight = VoxelFormat.sunlight(raw_data)
            future.blocklight = VoxelFormat.blocklight(raw_data)
            future.metaData = VoxelFormat.meta(raw_data)
        } else {
            // Build the raw data from the set parameters by editing the in-place data
            // (because we allow only editing some aspects of the getCell data)
            raw_data = cell_pre.data
            if (newVoxel != null) {
                raw_data = VoxelFormat.changeId(raw_data, world.contentTranslator.getIdForVoxel(newVoxel))
                future.voxel = newVoxel
            }
            if (sunlight >= 0) {
                raw_data = VoxelFormat.changeSunlight(raw_data, sunlight)
                future.sunlight = sunlight
            }
            if (blocklight >= 0) {
                raw_data = VoxelFormat.changeBlocklight(raw_data, blocklight)
                future.blocklight = blocklight
            }
            if (metadata >= 0) {
                raw_data = VoxelFormat.changeMeta(raw_data, metadata)
                future.metaData = metadata
            }
        }

        try {
            if (newVoxel == null || formerVoxel == newVoxel) {
                formerVoxel.onModification(cell_pre, future, cause)
            } else {
                formerVoxel.onRemove(cell_pre, cause)
                //newVoxel.onPlace(future, cause)

                raw_data = VoxelFormat.format(world.contentTranslator.getIdForVoxel(future.voxel), future.metaData, future.sunlight, future.blocklight)
            }
        } catch (e: WorldException) {
            // Abort !
            return if (return_context)
                cell_pre
            else
                null// throw e;
        }

        // Allocate if it makes sense
        if (voxelDataArray == null)
            voxelDataArray = atomicalyCreateInternalData()

        voxelDataArray!![x * 32 * 32 + y * 32 + z] = raw_data

        if (newVoxel != null && formerVoxel != newVoxel)
            newVoxel.whenPlaced(future)

        // Update lightning
        if (update)
            lightBaker.computeLightSpread(x, y, z, cell_pre.data, raw_data)

        // Increment the modifications counter
        compressionUncommitedModifications.incrementAndGet()
        revision.incrementAndGet()

        // Don't spam the thread creation spawn
        occlusion.requestUpdate()

        // Update related summary
        if (update)
            world.heightmapsManager.updateOnBlockPlaced(worldX, worldY, worldZ, future)

        // Mark the nearby chunks to be re-rendered
        if (update) {
            var sx = chunkX
            var ex = sx
            var sy = chunkY
            var ey = sy
            var sz = chunkZ
            var ez = sz

            if (x == 0)
                sx--
            else if (x == 31)
                ex++

            if (y == 0)
                sy--
            else if (y == 31)
                ey++

            if (z == 0)
                sz--
            else if (z == 31)
                ez++

            for (ix in sx..ex)
                for (iy in sy..ey)
                    for (iz in sz..ez) {
                        val chunk = world.chunksManager.getChunk(ix, iy, iz)
                        chunk?.mesh?.requestUpdate()
                    }
        }

        // If this is a 'master' world, notify remote users of the change !
        if (update && world is WorldMaster && world !is WorldTool) {
            val packet = PacketVoxelUpdate(
                    ActualChunkCell(this, chunkX * 32 + x, chunkY * 32 + y, chunkZ * 32 + z, raw_data))

            for (user in this.holder.users) {
                if (user !is RemotePlayer)
                    continue

                // Ignore clients that aren't playing
                val clientEntity = user.controlledEntity ?: continue

                user.pushPacket(packet)
            }
        }

        return if (return_context)
            ActualChunkCell(this, chunkX * 32 + x, chunkY * 32 + y, chunkZ * 32 + z, raw_data)
        else
            null
    }

    override fun getComponentsAt(x: Int, y: Int, z: Int): CellComponentsHolder {
        var x = x
        var y = y
        var z = z
        x = x and 0x1f
        y = y and 0x1f
        z = z and 0x1f

        val index = x * 1024 + y * 32 + z
        // System.out.println(index);

        var components: CellComponentsHolder? = allCellComponents[index]
        if (components == null) {
            components = CellComponentsHolder(this, index)
            allCellComponents[index] = components
        }
        return components
    }

    fun removeComponents(index: Int) {
        allCellComponents.remove(index)
    }

    private fun atomicalyCreateInternalData(): IntArray {
        chunkDataArrayCreation.acquireUninterruptibly()

        // If it's STILL null
        if (voxelDataArray == null)
            voxelDataArray = IntArray(32 * 32 * 32)

        chunkDataArrayCreation.release()

        return voxelDataArray!!
    }

    fun tick(tick: Long) {
        val stride = 8
        val offset = (tick % stride.toLong()).toInt()
        for(i in 0 until 32 * 32 * 32 / stride) {
            val j = i * stride + offset
            val x = j / 1024
            val y = (j / 32) % 32
            val z = (j) % 32

            val cell = peek(chunkX * 32 + x, chunkY * 32 + y, chunkZ * 32 + z)
            cell.voxel.tick(cell)
        }
    }

    override fun toString(): String {
        return "[CubicChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk + " light:" + this.lightBaker + "]"
    }

    fun holder(): ChunkHolderImplementation {
        return holder
    }

    override fun hashCode(): Int {
        return uuid
    }

    override fun destroy() {
        chunkDestructionSemaphore.acquireUninterruptibly()
        this.lightBaker.destroy()
        if (mesh is AutoRebuildingProperty)
            (this.mesh as AutoRebuildingProperty).destroy()
        this.isDestroyed = true
        //chunksCounter.decrementAndGet();
        chunkDestructionSemaphore.release()
    }

    override fun addEntity(entity: Entity) {
        entitiesLock.lock()
        localEntities.add(entity)
        entitiesLock.unlock()
    }

    override fun removeEntity(entity: Entity) {
        entitiesLock.lock()
        localEntities.remove(entity)
        entitiesLock.unlock()
    }

    companion object {
        val chunksCounter = AtomicInteger(0)
        val logger: Logger = LoggerFactory.getLogger("chunk")
    }
}
