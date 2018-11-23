//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.heightmap

import io.xol.chunkstories.api.math.Math2
import io.xol.chunkstories.api.player.Player
import io.xol.chunkstories.api.server.RemotePlayer
import io.xol.chunkstories.api.voxel.Voxel
import io.xol.chunkstories.api.voxel.VoxelFormat
import io.xol.chunkstories.api.voxel.VoxelSide
import io.xol.chunkstories.api.world.World
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.api.world.WorldUser
import io.xol.chunkstories.api.world.cell.Cell
import io.xol.chunkstories.api.world.cell.CellData
import io.xol.chunkstories.api.world.cell.FutureCell
import io.xol.chunkstories.api.world.heightmap.Heightmap
import io.xol.chunkstories.api.world.region.Region
import io.xol.chunkstories.net.packets.PacketHeightmap
import io.xol.chunkstories.util.concurrency.SimpleFence
import io.xol.chunkstories.world.WorldImplementation
import io.xol.chunkstories.world.generator.TaskGenerateWorldSlice
import io.xol.chunkstories.world.io.IOTaskLoadHeightmap
import io.xol.chunkstories.world.io.IOTaskSaveHeightmap
import net.jpountz.lz4.LZ4Factory
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * A region summary contains metadata about an 8x8 chunks ( or 256x256 blocks )
 * vertical slice of the world
 */
class HeightmapImplementation internal constructor(private val storage: HeightmapsStorage, override val regionX: Int, override val regionZ: Int, firstUser: WorldUser) : Heightmap {
    val world: WorldImplementation = storage.world

    private val stateLock = ReentrantLock()
    override lateinit var state: Heightmap.State
        private set

    private val usersSet = HashSet<WorldUser>()// new HashSet<WorldUser>();
    private val usersWaitingForIntialData = HashSet<RemotePlayer>()
    //private val usersLock = ReentrantLock()
    override var users: Set<WorldUser> = emptySet()
        private set

    val file: File?

    lateinit var heightData: IntArray private set
    lateinit var voxelData: IntArray private set

    lateinit var min: Array<IntArray> private set
    lateinit var max: Array<IntArray> private set

    // Textures (client renderer)
    //val texturesUpToDate = AtomicBoolean(false)

    init {
        if (world is WorldMaster) {
            file = File(world.folderPath + "/summaries/" + regionX + "." + regionZ + ".sum")

            if (file.exists()) {
                val task = IOTaskLoadHeightmap(this)
                state = Heightmap.State.Loading(task)
            } else {
                var dirX = 0
                var dirZ = 0

                if (firstUser is Player) {

                    val playerRegionX = Math2.floor(firstUser.controlledEntity!!.location.x() / 256)
                    val playerRegionZ = Math2.floor(firstUser.controlledEntity!!.location.z() / 256)

                    if (regionX < playerRegionX)
                        dirX = -1
                    if (regionX > playerRegionX)
                        dirX = 1

                    if (regionZ < playerRegionZ)
                        dirZ = -1
                    if (regionZ > playerRegionZ)
                        dirZ = 1
                }

                val task = TaskGenerateWorldSlice(world, this, dirX, dirZ)
                state = Heightmap.State.Generating(task)
                world.gameContext.tasks.scheduleTask(task)
            }
        } else {
            file = null
            state = Heightmap.State.Loading(SimpleFence())
        }

        this.registerUser(firstUser)
    }

    override fun registerUser(user: WorldUser): Boolean {
        try {
            stateLock.lock()
            if (usersSet.add(user)) {
                if (user is RemotePlayer) {
                    when (state) {
                        is Heightmap.State.Available -> user.pushPacket(PacketHeightmap(this))
                        is Heightmap.State.Loading -> this.usersWaitingForIntialData.add(user)
                    }
                }
                return true
            }

        } finally {
            stateLock.unlock()
        }

        return false
    }

    override fun unregisterUser(user: WorldUser): Boolean {
        try {
            stateLock.lock()
            usersSet.remove(user)

            if (usersSet.isEmpty()) {
                unload()
                return true
            }
            return false
        } finally {
            stateLock.unlock()
        }
    }

    internal fun unload() {
        fun remove() {
            if (!storage.removeSummary(this)) {
                println(this.toString() + " failed to be removed from the holder " + storage)
            }
            state = Heightmap.State.Zombie
        }

        try {
            stateLock.lock()

            when (state) {
                is Heightmap.State.Generating -> {
                    val task = (state as Heightmap.State.Generating).fence as TaskGenerateWorldSlice
                    if(task.tryCancel() || state !is Heightmap.State.Generating)
                        remove()
                    return
                }
                is Heightmap.State.Loading -> {
                    val task = (state as Heightmap.State.Loading).fence as IOTaskLoadHeightmap
                    if(task.tryCancel() || state !is Heightmap.State.Loading)
                        remove()
                    return
                }
                is Heightmap.State.Zombie -> throw Exception("Unloading a zombie heightmap !")
            }

            if(state !is Heightmap.State.Available && state !is Heightmap.State.Saving)
                throw Exception("Illegal state transition: When unloading, heightmap was in state $state")

            if(world is WorldMaster && state is Heightmap.State.Available) {
                val task = IOTaskSaveHeightmap(this)
                state = Heightmap.State.Saving(task)
                world.ioHandler.scheduleTask(task)

                return // we'll actually unload later
            }

            remove()

        } finally {
            stateLock.unlock()
        }

    }

    fun countUsers(): Int {
        return users.size
    }

    private fun index(x: Int, z: Int): Int {
        return x * 256 + z
    }

    fun updateOnBlockModification(worldX: Int, height: Int, worldZ: Int, cell: FutureCell) {
        var worldX = worldX
        var height = height
        var worldZ = worldZ

        worldX = worldX and 0xFF
        worldZ = worldZ and 0xFF

        val h = getHeight(worldX, worldZ)

        // If we place something solid over the last solid thing
        if (cell.voxel!!.solid || cell.voxel!!.name.endsWith("water")) {
            if (height >= h || h == Heightmap.NO_DATA) {
                heightData[index(worldX, worldZ)] = height
                voxelData[index(worldX, worldZ)] = cell.data
            }
        } else {
            // If removing the top block, start a loop to find bottom.
            if (height == h) {
                var raw_data = cell.data

                var loaded = false
                var solid = false
                var liquid = false
                do {
                    height--
                    loaded = world.isChunkLoaded(worldX / 32, height / 32, worldZ / 32)

                    val celli = world.peekSafely(worldX, height, worldZ)
                    solid = celli.voxel!!.solid
                    liquid = celli.voxel!!.name.endsWith("water")

                    raw_data = world.peekRaw(worldX, height, worldZ)
                } while (height >= 0 && loaded && !solid && !liquid)

                if (loaded) {
                    heightData[index(worldX, worldZ)] = height
                    voxelData[index(worldX, worldZ)] = raw_data
                }
            }
        }
    }

    override fun setTopCell(cell: CellData) {
        if (stateLock !is Heightmap.State.Available)
            return

        var worldX = cell.x
        var worldZ = cell.z
        val height = cell.y

        worldX = worldX and 0xFF
        worldZ = worldZ and 0xFF
        heightData[index(worldX, worldZ)] = height
        voxelData[index(worldX, worldZ)] = world.contentTranslator.getIdForVoxel(cell.voxel)
    }

    override fun getHeight(x: Int, z: Int): Int {
        if (state !is Heightmap.State.Available)
            return Heightmap.NO_DATA

        var x = x
        var z = z

        x = x and 0xFF
        z = z and 0xFF
        return heightData[index(x, z)]
    }

    fun getRawVoxelData(x: Int, z: Int): Int {
        if (state !is Heightmap.State.Available)
            return Heightmap.NO_DATA

        var x = x
        var z = z
        x = x and 0xFF
        z = z and 0xFF
        return voxelData[index(x, z)]
    }

    override fun getTopCell(x: Int, z: Int): CellData {
        val raw_data = getRawVoxelData(x, z)
        return SummaryCell(x, getHeight(x, z), z,
                world.contentTranslator.getVoxelForId(VoxelFormat.id(raw_data))!!, VoxelFormat.sunlight(raw_data),
                VoxelFormat.blocklight(raw_data), VoxelFormat.meta(raw_data))
    }

    internal inner class SummaryCell(x: Int, y: Int, z: Int, voxel: Voxel, meta: Int, blocklight: Int, sunlight: Int) : Cell(x, y, z, voxel, meta, blocklight, sunlight) {

        override fun getWorld(): World {
            return world
        }

        override fun getNeightbor(side_int: Int): CellData {
            val side = VoxelSide.values()[side_int]
            return getTopCell(x + side.dx, z + side.dz)
        }

    }

    private fun computeHeightMetadata() {
        if (state !is Heightmap.State.Available)
            return

        // Max mipmaps
        var resolution = 128
        var offset = 0
        while (resolution > 1) {
            for (x in 0 until resolution)
                for (z in 0 until resolution) {
                    // Fetch from the current resolution
                    // int v00 = heights[offset + (resolution * 2) * (x * 2) + (z * 2)];
                    // int v01 = heights[offset + (resolution * 2) * (x * 2) + (z * 2 + 1)];
                    // int v10 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2)];
                    // int v11 = heights[offset + (resolution * 2) * (x * 2 + 1) + (z * 2) + 1];

                    var maxIndex = 0
                    var maxHeight = 0
                    for (i in 0..1)
                        for (j in 0..1) {
                            val locationThere = offset + resolution * 2 * (x * 2 + i) + z * 2 + j
                            val heightThere = heightData!![locationThere]

                            if (heightThere >= maxHeight) {
                                maxIndex = locationThere
                                maxHeight = heightThere
                            }
                        }

                    // int maxHeight = max(max(v00, v01), max(v10, v11));

                    // Skip the already passed steps and the current resolution being sampled data
                    // to go write the next one
                    heightData[offset + resolution * 2 * (resolution * 2) + resolution * x + z] = maxHeight
                    voxelData[offset + resolution * 2 * (resolution * 2) + resolution * x + z] = voxelData!![maxIndex]
                }

            offset += resolution * 2 * resolution * 2
            resolution /= 2
        }
    }

    fun getHeightMipmapped(x: Int, z: Int, level: Int): Int {
        var x = x
        var z = z
        if (state !is Heightmap.State.Available)
            return Heightmap.NO_DATA
        if (level > 8)
            return Heightmap.NO_DATA
        val resolution = 256 shr level
        x = x shr level
        z = z shr level
        val offset = mainMimpmapOffsets[level]
        return heightData!![offset + resolution * x + z]
    }

    fun getDataMipmapped(x: Int, z: Int, level: Int): Int {
        var x = x
        var z = z
        if (state !is Heightmap.State.Available)
            return -1
        if (level > 8)
            return -1
        val resolution = 256 shr level
        x = x shr level
        z = z shr level
        val offset = mainMimpmapOffsets[level]
        return voxelData!![offset + resolution * x + z]
    }

    fun whenDataLoadedCallback(heightData: IntArray, voxelData: IntArray) {
        try {
            stateLock.lock()

            if (state !is Heightmap.State.Loading)
                throw Exception("Illegal state transition: When accepting loaded data, region was in state $state")

            // 512kb per summary, use of max mipmaps for heights
            this.heightData = IntArray(Math.ceil(256.0 * 256.0 * (1 + 1 / 3.0)).toInt())
            this.voxelData = IntArray(Math.ceil(256.0 * 256.0 * (1 + 1 / 3.0)).toInt())

            System.arraycopy(heightData, 0, this.heightData, 0, 256 * 256)
            System.arraycopy(voxelData, 0, this.voxelData, 0, 256 * 256)

            recomputeMetadata()
            state = Heightmap.State.Available()
        } finally {

            // Already have clients waiting for it ? Satisfy these messieurs
            // TODO copy list and then send so we block less
            for (user in usersWaitingForIntialData) {
                user.pushPacket(PacketHeightmap(this))
            }
            usersWaitingForIntialData.clear()

            stateLock.unlock()
        }
    }

    fun recomputeMetadata() {
        if(state !is Heightmap.State.Available)
            return

        this.computeHeightMetadata()
        this.computeMinMax()
    }

    private fun computeMinMax() {
        min = Array(8) { IntArray(8) }
        max = Array(8) { IntArray(8) }

        for (i in 0..7)
            for (j in 0..7) {
                var minl = Integer.MAX_VALUE
                var maxl = 0
                for (a in 0..31)
                    for (b in 0..31) {
                        val h = heightData[index(i * 32 + a, j * 32 + b)]
                        if (h > maxl)
                            maxl = h
                        if (h < minl)
                            minl = h
                    }
                min[i][j] = minl
                max[i][j] = maxl
            }

    }

    override fun toString(): String {
        return ("[Heightmap x:$regionX z:$regionZ users: ${this.countUsers()} state:$state]")
    }

    companion object {

        // LZ4 compressors & decompressors
        internal var factory = LZ4Factory.fastestInstance()
        var compressor = factory.highCompressor(10)
        var decompressor = factory.fastDecompressor()

        /**
         * The offsets in an array containing sequentially each mipmaps of a square
         * texture of base size 256
         */
        val mainMimpmapOffsets = intArrayOf(0, 65536, 81920, 86016, 87040, 87296, 87360, 87376, 87380, 87381)

        /**
         * The offsets in an array containing sequentially each mipmaps of a square
         * texture of base size 128
         */
        val minHeightMipmapOffsets = intArrayOf(0, 16384, 20480, 21504, 21760, 21824, 21840, 21844, 21845)
    }
}
