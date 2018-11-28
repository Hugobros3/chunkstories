//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.heightmap

import io.xol.chunkstories.api.Location
import io.xol.chunkstories.api.world.WorldUser
import io.xol.chunkstories.api.world.cell.CellData
import io.xol.chunkstories.api.world.cell.FutureCell
import io.xol.chunkstories.api.world.heightmap.Heightmap
import io.xol.chunkstories.api.world.heightmap.WorldHeightmaps
import io.xol.chunkstories.world.WorldImplementation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

class HeightmapsStorage(override val world: WorldImplementation) : WorldHeightmaps {
    private val worldSize: Int = world.sizeInChunks * 32
    private val worldSizeInChunks: Int = world.sizeInChunks
    private val worldSizeInRegions: Int = world.sizeInChunks / 8

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
        worldX = sanitizeHorizontalCoordinate(worldX)
        worldZ = sanitizeHorizontalCoordinate(worldZ)
        return acquireHeightmap(worldUser, worldX / 256, worldZ / 256)
    }

    override fun acquireHeightmapLocation(worldUser: WorldUser, location: Location): HeightmapImplementation {
        return acquireHeightmap(worldUser, location.x().toInt(), location.z().toInt())
    }

    override fun getHeightmap(regionX: Int, regionZ: Int): Heightmap? {
        return getHeightmapWorldCoordinates(regionX * 256, regionZ * 256)
    }

    override fun getHeightmapChunkCoordinates(chunkX: Int, chunkZ: Int): Heightmap? {
        return getHeightmapWorldCoordinates(chunkX * 32, chunkZ * 32)
    }

    override fun getHeightmapLocation(location: Location): Heightmap? {
        return getHeightmapWorldCoordinates(location.x().toInt(), location.z().toInt())
    }

    override fun getHeightmapWorldCoordinates(worldX: Int, worldZ: Int): HeightmapImplementation? {
        var worldX = worldX
        var worldZ = worldZ
        worldX = sanitizeHorizontalCoordinate(worldX)
        worldZ = sanitizeHorizontalCoordinate(worldZ)

        val i = index(worldX, worldZ)

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

    fun getHeightMipmapped(x: Int, z: Int, level: Int): Int {
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
    }

    override fun getHeightAtWorldCoordinates(x: Int, z: Int): Int {
        var x = x
        var z = z
        x %= worldSize
        z %= worldSize
        if (x < 0)
            x += worldSize
        if (z < 0)
            z += worldSize
        val cs = getHeightmapWorldCoordinates(x, z) ?: return Heightmap.NO_DATA
        return cs.getHeight(x % 256, z % 256)
    }

    fun getRawDataAtWorldCoordinates(x: Int, z: Int): Int {
        var x = x
        var z = z
        x %= worldSize
        z %= worldSize
        if (x < 0)
            x += worldSize
        if (z < 0)
            z += worldSize
        val cs = getHeightmapWorldCoordinates(x, z) ?: return 0
        return cs.getRawVoxelData(x % 256, z % 256)
    }

    override fun getTopCellAtWorldCoordinates(x: Int, z: Int): CellData {
        var x = x
        var z = z
        x %= worldSize
        z %= worldSize
        if (x < 0)
            x += worldSize
        if (z < 0)
            z += worldSize
        val cs = getHeightmapWorldCoordinates(x, z) ?: return TODO()
        return cs.getTopCell(x, z)
    }

    fun updateOnBlockPlaced(x: Int, y: Int, z: Int, future: FutureCell) {
        var x = x
        var z = z
        x %= worldSize
        z %= worldSize
        if (x < 0)
            x += worldSize
        if (z < 0)
            z += worldSize
        val summary = getHeightmapWorldCoordinates(x, z)

        summary?.updateOnBlockModification(x % 256, y, z % 256, future)
    }

    /*fun saveAllLoadedSummaries(): Fence {
        val allSummariesSaves = CompoundFence()
        for (cs in heightmapData.values) {
            allSummariesSaves.add(cs.save())
        }

        return allSummariesSaves
    }*/

    internal fun removeSummary(regionSummary: HeightmapImplementation): Boolean {
        try {
            heightmapsLock.writeLock().lock()
            return heightmapData.remove(this.index(regionSummary.regionX * 256, regionSummary.regionZ * 256)) != null
        } finally {
            heightmapsLock.writeLock().unlock()
        }
    }

    private fun sanitizeHorizontalCoordinate(coordinate: Int): Int {
        var coordinate = coordinate
        coordinate %= (world.sizeInChunks * 32)
        if (coordinate < 0)
            coordinate += world.sizeInChunks * 32
        return coordinate
    }

    fun all(): Collection<HeightmapImplementation> {
        return this.heightmapData.values
    }
}
