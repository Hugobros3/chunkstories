//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.storage

import io.xol.chunkstories.api.util.concurrency.Fence
import io.xol.chunkstories.api.world.WorldUser
import io.xol.chunkstories.api.world.chunk.ChunkHolder
import io.xol.chunkstories.api.world.region.Region
import io.xol.chunkstories.util.concurrency.CompoundFence
import io.xol.chunkstories.util.concurrency.TrivialFence
import io.xol.chunkstories.world.WorldImplementation
import io.xol.chunkstories.world.chunk.CubicChunk
import io.xol.chunkstories.world.generator.TaskGenerateWorldSlice
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock

class RegionsStorage(val world: WorldImplementation) {

    private val regionsLock = ReentrantReadWriteLock()

    private val regionsMap: MutableMap<Int, RegionImplementation> = mutableMapOf()
    var regionsList: List<RegionImplementation> = emptyList()
        private set

    private val sizeInRegions = world.worldInfo.size.sizeInChunks / 8
    private val heightInRegions = world.worldInfo.size.heightInChunks / 8

    /** Regions require a reference to their heightmap data, but the heightmap initial data creation process will also require a reference to all the regions
     * vertically encompassed by it. To solve this loop, we first acquire the heightmap data and pass it to the region constructor, using this dummy user.*/
    val bootStrapper = object : WorldUser {}

    fun getRegionChunkCoordinates(chunkX: Int, chunkY: Int, chunkZ: Int): RegionImplementation? {
        return getRegion(chunkX / 8, chunkY / 8, chunkZ / 8)
    }

    fun getRegion(regionX: Int, regionY: Int, regionZ: Int): RegionImplementation? {
        try {
            regionsLock.readLock().lock()
            val key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY
            val region = regionsMap[key]

            if(region?.state is Region.State.Zombie)
                return null

            return region
        } finally {
            regionsLock.readLock().unlock()
        }
    }

    fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int): CubicChunk? {
        val holder = getRegionChunkCoordinates(chunkX, chunkY, chunkZ)
        return holder?.getChunk(chunkX, chunkY, chunkZ)
    }

    fun saveAll() = TrivialFence()

    override fun toString(): String {
        return "[RegionsHolder: " + regionsList.size + " loaded regions]"
    }

    fun countChunks(): Int = regionsList.size

    fun acquireRegion(user: WorldUser, regionX: Int, regionY: Int, regionZ: Int): RegionImplementation {
        if (regionY < 0 || regionY > world.maxHeight / 256)
            throw Exception("Out of bounds: RegionY = $regionY is out of world bounds.")

        val key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY
        try {
            this.regionsLock.writeLock().lock()

            // In the special case where the user is the task that generates the map itself, acquiring the map will send us in a loop.
            // To solve this, we simply take the heightmap field from the task.
            val heightmap = if (user is TaskGenerateWorldSlice)
                user.heightmap
            else
                world.regionsSummariesHolder.acquireHeightmap(bootStrapper, regionX, regionZ)

            var region: RegionImplementation? = regionsMap[key]
            var fresh = false

            if(region != null) {
                region.stateLock.lock()
                if(region.state is Region.State.Zombie) {
                    //logger.debug("Region is zombie, killing it")
                    region.stateLock.unlock()
                    removeRegion(region)
                    region = null
                }
            }

            if (region == null) {
                region = RegionImplementation(world, heightmap, regionX, regionY, regionZ)
                fresh = true
            }

            val userAdded = region.registerUser(user)

            if (fresh) {

                //regionsMap[key] = region

                if(regionsMap.putIfAbsent(key, region) != null) {
                    throw Exception("Overwriting a sane region, wtf !")
                }
                regionsList = regionsMap.values.toList()
            } else {
                region.stateLock.unlock()
            }

            if (user !is TaskGenerateWorldSlice)
                heightmap.unregisterUser(bootStrapper)

            return region
        } finally {
            this.regionsLock.writeLock().unlock()
        }
    }

    fun acquireChunkHolder(user: WorldUser, chunkX: Int, chunkY: Int, chunkZ: Int): ChunkHolder {
        if (chunkY < 0 || chunkY > world.maxHeight / 32)
            throw Exception("Out of bounds: ChunkY = $chunkY is out of world bounds.")

        val regionX = chunkX shr 3
        val regionY = chunkY shr 3
        val regionZ = chunkZ shr 3


        val key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY
        try {
            this.regionsLock.writeLock().lock()

            // Unlike the other entry point, this doesn't pose a risk of loop
            val heightmap = world.regionsSummariesHolder.acquireHeightmap(bootStrapper, regionX, regionZ)

            val ogRegion = regionsMap[key]
            var region: RegionImplementation? = ogRegion
            var fresh = false

            if(region != null) {
                region.stateLock.lock()
                if(region.state is Region.State.Zombie) {
                    //logger.debug("Region is zombie, killing it")
                    region.stateLock.unlock()
                    removeRegion(region)
                    region = null
                }
            }

            if (region == null) {
                region = RegionImplementation(world, heightmap, regionX, regionY, regionZ)
                fresh = true
            }

            val chunkHolder = region.getChunkHolder(chunkX, chunkY, chunkZ)
            val userAdded = chunkHolder.registerUser(user)

            if (fresh) {
                if(regionsMap.putIfAbsent(key, region) != null) {
                    throw Exception("Overwriting a sane region, wtf ! $ogRegion $region")
                }
                regionsList = regionsMap.values.toList()
            } else {
                region.stateLock.unlock()
            }

            if (user !is TaskGenerateWorldSlice)
                heightmap.unregisterUser(bootStrapper)

            return region.getChunkHolder(chunkX, chunkY, chunkZ)
        } finally {
            this.regionsLock.writeLock().unlock()
        }
    }

    /**
     * Callback by the holder's unload() method to remove himself from this list.
     */
    internal fun removeRegion(region: RegionImplementation) {
        this.regionsLock.writeLock().lock()
        //val key = (region.regionX * sizeInRegions + region.regionY) * heightInRegions + region.regionZ
        val key = (region.regionX * sizeInRegions + region.regionZ) * heightInRegions + region.regionY
        regionsMap.remove(key)
        regionsList = regionsMap.values.toList()
        this.regionsLock.writeLock().unlock()
    }

    companion object {
        val logger = LoggerFactory.getLogger("world.storage")
    }
}
