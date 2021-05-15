//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.heightmap

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.world.WorldUser
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.api.world.heightmap.WorldHeightmapsManager
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.sanitizeHorizontalCoordinate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

class HeightmapsStorage(override val world: WorldImplementation) : WorldHeightmapsManager {
    private val worldSize: Int = world.properties.size.squareSizeInBlocks
    private val worldSizeInChunks: Int = worldSize / 32
    private val worldSizeInRegions: Int = worldSizeInChunks / 8

    private val heightmapData = ConcurrentHashMap<Long, HeightmapImplementation>()
    private val heightmapsLock = ReentrantReadWriteLock()

    internal fun index(x: Int, z: Int): Long {
        val rx = x / 256
        val rz = z / 256
        return (rx * worldSizeInRegions + rz).toLong()
    }

    fun Int.sanitizeRegionCoordinates(): Int {
        val mod = this % worldSizeInRegions
        if (mod < 0)
            return mod + worldSizeInRegions
        else
            return mod
    }

    override fun acquireHeightmap(worldUser: WorldUser, regionX: Int, regionZ: Int): HeightmapImplementation {
        var regionX = regionX
        var regionZ = regionZ

        regionX %= worldSizeInRegions
        regionZ %= worldSizeInRegions
        if (regionX < 0)
            regionX += worldSizeInRegions
        if (regionZ < 0)
            regionZ += worldSizeInRegions

        val index = index(regionX * 256, regionZ * 256)

        try {
            heightmapsLock.writeLock().lock()

            var heightmap = heightmapData[index]

            if (heightmap != null && heightmap.state is Heightmap.State.Zombie) {
                heightmap = null
                heightmapData.remove(index)
            }

            if (heightmap == null) {
                heightmap = HeightmapImplementation(this, regionX, regionZ, worldUser)
                if (heightmapData.putIfAbsent(index, heightmap) != null)
                    throw Exception("Overwriting existing heightmap !")
            } else {
                heightmap.registerUser(worldUser)
            }

            return heightmap
        } finally {
            heightmapsLock.writeLock().unlock()
        }
    }

    override fun acquireHeightmapChunkCoordinates(worldUser: WorldUser, chunkX: Int, chunkZ: Int): HeightmapImplementation {
        var sanitizedChunkX = chunkX
        var sanitizedChunkZ = chunkZ
        sanitizedChunkX %= worldSizeInChunks
        sanitizedChunkZ %= worldSizeInChunks
        if (sanitizedChunkX < 0)
            sanitizedChunkX += worldSizeInChunks
        if (sanitizedChunkZ < 0)
            sanitizedChunkZ += worldSizeInChunks

        return acquireHeightmap(worldUser, sanitizedChunkX / 8, sanitizedChunkZ / 8)
    }

    override fun acquireHeightmapWorldCoordinates(worldUser: WorldUser, worldX: Int, worldZ: Int): HeightmapImplementation {
        var worldX = worldX
        var worldZ = worldZ
        worldX = world.sanitizeHorizontalCoordinate(worldX)
        worldZ = world.sanitizeHorizontalCoordinate(worldZ)
        return acquireHeightmap(worldUser, worldX / 256, worldZ / 256)
    }

    override fun acquireHeightmapLocation(worldUser: WorldUser, location: Location): HeightmapImplementation {
        return acquireHeightmap(worldUser, location.x().toInt(), location.z().toInt())
    }

    override fun getHeightmap(regionX: Int, regionZ: Int): HeightmapImplementation? {
        return getHeightmapWorldCoordinates(regionX * 256, regionZ * 256)
    }

    override fun getHeightmapChunkCoordinates(chunkX: Int, chunkZ: Int): HeightmapImplementation? {
        return getHeightmapWorldCoordinates(chunkX * 32, chunkZ * 32)
    }

    override fun getHeightmapLocation(location: Location): HeightmapImplementation? {
        return getHeightmapWorldCoordinates(location.x().toInt(), location.z().toInt())
    }

    override fun getHeightmapWorldCoordinates(worldX: Int, worldZ: Int): HeightmapImplementation? {
        val sx = world.sanitizeHorizontalCoordinate(worldX)
        val sz = world.sanitizeHorizontalCoordinate(worldZ)

        val i = index(sx, sz)

        try {
            //heightmapsLock.readLock().lock()
            val heightmap = heightmapData[i]
            return heightmap?.let {
                when {
                    it.state is Heightmap.State.Zombie -> null
                    it.state is Heightmap.State.Generating -> it
                    it.state is Heightmap.State.Loading -> null
                    else -> it
                }
            }
        } finally {
            //heightmapsLock.readLock().unlock()
        }
    }

    /*fun getHeightMipmapped(x: Int, z: Int, level: Int): Int {
        var x = x
        var z = z
        x %= worldSize
        z %= worldSize
        if (x < 0)
            x += worldSize
        if (z < 0)
            z += worldSize
        val cs = getHeightmapWorldCoordinates(x, z) ?: return Heightmap.NO_DATA
        return cs.getHeightMipmapped(x % 256, z % 256, level)
    }

    fun getDataMipmapped(x: Int, z: Int, level: Int): Int {
        var x = x
        var z = z
        x %= worldSize
        z %= worldSize
        if (x < 0)
            x += worldSize
        if (z < 0)
            z += worldSize
        val cs = getHeightmapWorldCoordinates(x, z) ?: return 0
        return cs.getDataMipmapped(x % 256, z % 256, level)
    }*/

    override fun getHeight(x: Int, z: Int): Int {
        val sx = world.sanitizeHorizontalCoordinate(x)
        val sz = world.sanitizeHorizontalCoordinate(z)
        val heightmap = getHeightmapWorldCoordinates(x, z) ?: return -1
        return heightmap.getHeight(sx, sz)
    }

    override fun getBlockType(x: Int, z: Int): BlockType {
        val sx = world.sanitizeHorizontalCoordinate(x)
        val sz = world.sanitizeHorizontalCoordinate(z)
        val heightmap = getHeightmapWorldCoordinates(x, z) ?: return world.gameInstance.content.blockTypes.air
        return heightmap.getBlockType(sx, sz)
    }

    fun updateOnBlockPlaced(x: Int, y: Int, z: Int, cellData: CellData) {
        val sx = world.sanitizeHorizontalCoordinate(x)
        val sz = world.sanitizeHorizontalCoordinate(z)
        val heightmap = getHeightmapWorldCoordinates(x, z) ?: return
        heightmap.updateOnBlockModification(x, y, z, cellData)
    }

    internal fun remove(regionSummary: HeightmapImplementation): Boolean {
        try {
            heightmapsLock.writeLock().lock()
            return heightmapData.remove(this.index(regionSummary.regionX * 256, regionSummary.regionZ * 256)) != null
        } finally {
            heightmapsLock.writeLock().unlock()
        }
    }

    fun all(): Collection<HeightmapImplementation> {
        return this.heightmapData.values
    }
}