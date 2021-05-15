//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.chunk

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.block.BlockAdditionalData
import xyz.chunkstories.api.content.json.*
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.cell.PodCellData
import xyz.chunkstories.api.world.chunk.*
import xyz.chunkstories.api.world.region.Region
import xyz.chunkstories.block.VoxelFormat
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty
import xyz.chunkstories.world.chunk.deriveddata.ChunkOcclusionProperty
import xyz.chunkstories.world.region.RegionImplementation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ChunkImplementation constructor(override val holder: ChunkHolderImplementation, override val chunkX: Int, override val chunkY: Int, override val chunkZ: Int, compressedData: ChunkCompressedData?) : Chunk {
    override val world: WorldImplementation
    protected val holdingRegion: RegionImplementation

    public var blockData: IntArray? = null
    val allCellComponents = mutableMapOf<Int, MutableList<BlockAdditionalData>>()

    // Count unsaved edits atomically, fancy :]
    val compressionUncommitedModifications = AtomicInteger()
    val revision = AtomicLong(0)

    val occlusion: ChunkOcclusionManager
    val lightBaker: ChunkLightBaker
    lateinit var mesh: ChunkMesh

    // Set after destroy()
    var isDestroyed = false
    val chunkDestructionSemaphore = Semaphore(1)

    val localEntities: MutableSet<Entity> = ConcurrentHashMap.newKeySet()

    val isAirChunk: Boolean
        get() = blockData == null

    override val region: Region
        get() = holdingRegion

    override val entitiesWithinChunk: Collection<Entity>
        get() = localEntities

    init {
        chunksCounter.incrementAndGet()

        this.holdingRegion = holder.region
        this.world = holdingRegion.world

        occlusion = ChunkOcclusionProperty(this)
        lightBaker = ChunkLightBaker(this)

        if (compressedData is ChunkCompressedData.NonAir) {
            this.blockData = compressedData.extractVoxelData()

            val extendedData = compressedData.extractVoxelExtendedData()
            for (cellWithExtendedData in extendedData.elements) {
                /*val index = (cellWithExtendedData as? Json.Dict ?: continue)["index"].asInt!!

                val components = CellComponentsHolder(this, index)
                allCellComponents[index] = components

                // Call the block's onPlace method as to make it spawn the necessary components
                val peek = getCell(components.x, components.y, components.z)
                val future = FreshFutureCell(this, peek)
                //peek.voxel.whenPlaced(future)

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
                }*/
                TODO("Load components")
            }

            val savedEntities = compressedData.extractEntities()
            for (savedEntity in savedEntities.elements) {
                val entity = EntitySerialization.deserializeEntity(world, savedEntity)
                // TODO this should be the world's responsability
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

    override fun getCell(x: Int, y: Int, z: Int): ChunkCellProxy {
        return ChunkCellProxy(x, y, z, false)
    }

    override fun getCellMut(x: Int, y: Int, z: Int): MutableChunkCell {
        TODO("Not yet implemented")
    }

    inner class ChunkCellProxy(override val x: Int, override val y: Int, override val z: Int, val mutable: Boolean): MutableChunkCell {
        override val chunk: Chunk
            get() = this@ChunkImplementation

        override val world: World
            get() = this@ChunkImplementation.world

        override var data: CellData
            get() = getCellData(x, y, z)
            set(value) = setCellData(x, y, z, data)

        override val additionalData: MutableMap<String, BlockAdditionalData>
            get() = TODO("Not yet implemented")

        override fun registerAdditionalData(name: String, data: BlockAdditionalData) {
            TODO("Not yet implemented")
        }

        override fun unregisterAdditionalData(name: String): Boolean {
            TODO("Not yet implemented")
        }
    }

    /*override fun peekRaw(x: Int, y: Int, z: Int): Int {
        var x = x
        var y = y
        var z = z
        x = sanitizeCoordinate(x)
        y = sanitizeCoordinate(y)
        z = sanitizeCoordinate(z)

        if (blockData == null) {
            // Empty chunk ?
            // Use the heightmap to figure out wether or not that getCell should be skylit.
            var sunlight = 0
            val groundHeight = holdingRegion.heightmap.getHeight(x, z)
            //int groundHeight = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(chunkX * 32 + x, chunkZ * 32 + z);
            if (groundHeight < y + chunkY * 32 && groundHeight != Heightmap.NO_DATA)
                sunlight = 15

            return VoxelFormat.format(0, 0, sunlight, 0)
        } else {
            return blockData!![x * 32 * 32 + y * 32 + z]
        }
    }

    override fun pokeRaw(x: Int, y: Int, z: Int, raw_data_bits: Int) {
        pokeInternal(x, y, z, null, 0, 0, 0, raw_data_bits, true, true, false, null)
    }*/

    /*/**
     * The 'core' of the core, this private function is responsible for placing and
     * keeping everyone up to snuff on block modifications. It all comes back to
     * this really.
     */
    private fun pokeInternal(worldX: Int, worldY: Int, worldZ: Int, newVoxel: BlockType?,
                             sunlight: Int, blocklight: Int, metadata: Int, raw_data: Int, use_raw_data: Boolean,
                             update: Boolean, return_context: Boolean) {
        val x = sanitizeCoordinate(worldX)
        val y = sanitizeCoordinate(worldY)
        val z = sanitizeCoordinate(worldZ)

        val cell_pre = peek(x, y, z)
        val formerVoxel = cell_pre.voxel

        val future = PodCellData()

        if (use_raw_data) {
            // We need this for voxel placement logic
            newVoxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(raw_data))
            // Build the future from parsing the raw data
            newVoxel?.let { future.voxel = it }
            future.sun = VoxelFormat.sunlight(raw_data)
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

        if (newVoxel == null || formerVoxel == newVoxel) {
            formerVoxel.onModification(cell_pre, future, cause)
        } else {
            formerVoxel.onRemove(cell_pre, cause)
            //newVoxel.onPlace(future, cause)

            raw_data = VoxelFormat.format(world.contentTranslator.getIdForVoxel(future.voxel), future.metaData, future.sunlight, future.blocklight)
        }

        // Allocate if it makes sense
        if (blockData == null)
            blockData = atomicalyCreateInternalData()

        blockData!![x * 32 * 32 + y * 32 + z] = raw_data

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
            /*val packet = PacketVoxelUpdate(ActualChunkCell(this, chunkX * 32 + x, chunkY * 32 + y, chunkZ * 32 + z, raw_data))

            for (user in this.holder.users) {
                if (user !is RemotePlayer)
                    continue

                // Ignore clients that aren't playing
                val clientEntity = user.controlledEntity ?: continue

                user.pushPacket(packet)
            }*/
            TODO("fix networking")
        }
    }*/

    override fun getCellData(x: Int, y: Int, z: Int): CellData {
        val air = world.gameInstance.content.blockTypes.air
        if (blockData == null)
            return PodCellData(air)
        val compressed = blockData!![x * 32 * 32 + y * 32 + z]
        return PodCellData(blockType = world.contentTranslator.getVoxelForId(VoxelFormat.id(compressed)) ?: air,
            sunlightLevel = VoxelFormat.sunlight(compressed),
            blocklightLevel = VoxelFormat.blocklight(compressed),
            extraData = VoxelFormat.meta(compressed))
    }

    override fun setCellData(x: Int, y: Int, z: Int, data: CellData) {
        setCellDataSilent(x, y, z, data)
    }

    fun setCellDataSilent(x: Int, y: Int, z: Int, data: CellData) {
        val compressed = VoxelFormat.format(data.blockType.assignedId, data.extraData, data.sunlightLevel, data.blocklightLevel)
        if (blockData == null)
            blockData = IntArray(32 * 32 * 32)
        blockData!![x * 32 * 32 + y * 32 + z] = compressed
    }

    fun removeComponents(index: Int) {
        allCellComponents.remove(index)
    }

    fun tick(tick: Long) {
        val stride = 8
        val offset = (tick % stride.toLong()).toInt()
        for (i in 0 until 32 * 32 * 32 / stride) {
            val j = i * stride + offset
            val x = j / 1024
            val y = (j / 32) % 32
            val z = (j) % 32

            val cell = getCell(chunkX * 32 + x, chunkY * 32 + y, chunkZ * 32 + z)
            cell.data.blockType.tick(cell)
        }
    }

    override fun toString(): String {
        return "[CubicChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk + " light:" + this.lightBaker + "]"
    }

    fun holder(): ChunkHolderImplementation {
        return holder
    }

    fun destroy() {
        chunkDestructionSemaphore.acquireUninterruptibly()
        this.lightBaker.destroy()
        if (mesh is AutoRebuildingProperty)
            (this.mesh as AutoRebuildingProperty).destroy()
        this.isDestroyed = true
        //chunksCounter.decrementAndGet();
        chunkDestructionSemaphore.release()
    }

    /*override fun addEntity(entity: Entity) {
        entitiesLock.lock()
        localEntities.add(entity)
        entitiesLock.unlock()
    }

    override fun removeEntity(entity: Entity) {
        entitiesLock.lock()
        localEntities.remove(entity)
        entitiesLock.unlock()
    }*/

    companion object {
        val chunksCounter = AtomicInteger(0)
        val logger: Logger = LoggerFactory.getLogger("chunk")
    }
}
