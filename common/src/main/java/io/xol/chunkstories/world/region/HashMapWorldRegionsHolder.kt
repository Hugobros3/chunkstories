//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.region

import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

import io.xol.chunkstories.api.util.concurrency.Fence
import io.xol.chunkstories.api.world.WorldUser
import io.xol.chunkstories.api.world.chunk.ChunkHolder
import io.xol.chunkstories.util.concurrency.CompoundFence
import io.xol.chunkstories.world.WorldImplementation
import io.xol.chunkstories.world.chunk.CubicChunk

class HashMapWorldRegionsHolder(private val world: WorldImplementation) {

    private val regionsLock = ReentrantReadWriteLock()

    private val regions: MutableMap<Int, RegionImplementation> = mutableMapOf()
    private var regionsList: List<RegionImplementation> = emptyList()

    private val sizeInRegions = world.worldInfo.size.sizeInChunks / 8
    private val heightInRegions = world.worldInfo.size.heightInChunks / 8

    val stats: String
        get() = countChunks().toString() + " (lr: " + regions.size + " )"

    //TODO have a copyOnWrite list or smth
    fun internalGetLoadedRegions(): Collection<RegionImplementation> = regionsList
    /*{
        try {
            regionsLock.readLock().lock()
            val list = ArrayList<RegionImplementation>()
            list.addAll(regions.values)
            return list
        } finally {
            regionsLock.readLock().unlock()
        }
    }*/

    fun internalGetLoadedChunks(): Collection<CubicChunk> = internalGetLoadedRegions().flatMap { it.loadedChunks }

    fun getRegionChunkCoordinates(chunkX: Int, chunkY: Int, chunkZ: Int): RegionImplementation? {
        return getRegion(chunkX / 8, chunkY / 8, chunkZ / 8)
    }

    fun getRegion(regionX: Int, regionY: Int, regionZ: Int): RegionImplementation? {
        try {
            regionsLock.readLock().lock()
            val key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY
            return regions[key]
        } finally {
            regionsLock.readLock().unlock()
        }
    }

    fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int): CubicChunk? {
        val holder = getRegionChunkCoordinates(chunkX, chunkY, chunkZ)
        return holder?.getChunk(chunkX, chunkY, chunkZ)
    }

    fun saveAll(): Fence {
        val allRegionsFences = CompoundFence()
        try {
            regionsLock.readLock().lock()
            regionsList.mapTo(allRegionsFences) { it.save() }
            return allRegionsFences
        } finally {
            regionsLock.readLock().unlock()
        }
    }

    fun destroy() {
        regions.clear()
    }

    override fun toString(): String {
        return "[RegionsHolder: " + regions.size + " loaded regions]"
    }

    fun countChunks() : Int = regionsList.size

    /**
     * Atomically adds an user to a region itself, and creates it if it was
     * previously unused
     */
    fun acquireRegion(user: WorldUser, regionX: Int, regionY: Int, regionZ: Int): RegionImplementation? {
        if (regionY < 0 || regionY > world.maxHeight / 256)
            return null

        val key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY

        this.regionsLock.writeLock().lock()

        var region: RegionImplementation? = regions[key]
        var fresh = false
        if (region == null) {
            region = RegionImplementation(world, regionX, regionY, regionZ)
            fresh = true
        }

        val userAdded = region.registerUser(user)

        if (fresh) {
            regions[key] = region
            regionsList = regions.values.toList()
        }

        this.regionsLock.writeLock().unlock()

        return if (userAdded) region else null
    }

    /**
     * Atomically adds an user to a region's chunk, and creates the region if it was
     * previously unused
     */
    fun acquireChunkHolder(user: WorldUser, chunkX: Int, chunkY: Int, chunkZ: Int): ChunkHolder? {
        if (chunkY < 0 || chunkY > world.maxHeight / 32)
            return null

        val regionX = chunkX shr 3
        val regionY = chunkY shr 3
        val regionZ = chunkZ shr 3
        val key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY

        this.regionsLock.writeLock().lock()

        var region: RegionImplementation? = regions[key]
        var fresh = false
        if (region == null) {
            region = RegionImplementation(world, regionX, regionY, regionZ)
            fresh = true
        }

        val chunkHolder = region.getChunkHolder(chunkX, chunkY, chunkZ)
        val userAdded = chunkHolder.registerUser(user)

        if (fresh) {
            regions[key] = region
            regionsList = regions.values.toList()
        }

        this.regionsLock.writeLock().unlock()

        return if (userAdded) chunkHolder else chunkHolder
    }

    /**
     * Callback by the holder's unload() method to remove himself from this list.
     */
    fun removeRegion(region: RegionImplementation) {
        this.regionsLock.writeLock().lock()
        val key = (region.getRegionX() * sizeInRegions + region.getRegionZ()) * heightInRegions + region.getRegionY()
        regions.remove(key)
        regionsList = regions.values.toList()
        this.regionsLock.writeLock().unlock()
    }
}
