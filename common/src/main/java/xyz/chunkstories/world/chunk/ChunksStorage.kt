package xyz.chunkstories.world.chunk

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.world.WorldUser
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.chunk.WorldChunksManager
import xyz.chunkstories.world.WorldImplementation

class ChunksStorage(override val world: WorldImplementation) : WorldChunksManager {
    val sizeInChunks = world.sizeInChunks

    override fun acquireChunkHolderLocation(user: WorldUser, location: Location): ChunkHolder {
        return acquireChunkHolder(user, location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    override fun acquireChunkHolder(user: WorldUser, chunkX: Int, chunkY: Int, chunkZ: Int): ChunkHolder {
        var chunkX = chunkX
        var chunkZ = chunkZ
        // Sanitation of input data
        chunkX %= sizeInChunks
        chunkZ %= sizeInChunks
        if (chunkX < 0)
            chunkX += sizeInChunks
        if (chunkZ < 0)
            chunkZ += sizeInChunks
        return world.regionsManager.acquireChunkHolder(user, chunkX, chunkY, chunkZ)
    }

    override fun acquireChunkHolderWorldCoordinates(user: WorldUser, worldX: Int, worldY: Int, worldZ: Int): ChunkHolder {
        var worldX = worldX
        var worldY = worldY
        var worldZ = worldZ
        worldX = world.sanitizeHorizontalCoordinate(worldX)
        worldY = world.sanitizeVerticalCoordinate(worldY)
        worldZ = world.sanitizeHorizontalCoordinate(worldZ)

        return world.regionsManager.acquireChunkHolder(user, worldX / 32, worldY / 32, worldZ / 32)
    }

    override fun isChunkLoaded(chunkX: Int, chunkY: Int, chunkZ: Int): Boolean {
        var chunkX = chunkX
        var chunkZ = chunkZ
        // Sanitation of input data
        chunkX %= sizeInChunks
        chunkZ %= sizeInChunks
        if (chunkX < 0)
            chunkX += sizeInChunks
        if (chunkZ < 0)
            chunkZ += sizeInChunks
        // Out of bounds checks
        if (chunkY < 0)
            return false
        return if (chunkY >= world.worldInfo.size.heightInChunks) false else world.regionsManager.getChunk(chunkX, chunkY, chunkZ) != null
        // If it doesn't return null then it exists
    }

    override fun getChunkWorldCoordinates(location: Location): ChunkImplementation? {
        return getChunkWorldCoordinates(location.x().toInt(), location.y().toInt(),
                location.z().toInt())
    }

    override fun getChunkWorldCoordinates(worldX: Int, worldY: Int, worldZ: Int): ChunkImplementation? {
        return getChunk(worldX / 32, worldY / 32, worldZ / 32)
    }

    override fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int): ChunkImplementation? {
        var chunkX = chunkX
        var chunkZ = chunkZ
        chunkX %= sizeInChunks
        chunkZ %= sizeInChunks
        if (chunkX < 0)
            chunkX += sizeInChunks
        if (chunkZ < 0)
            chunkZ += sizeInChunks
        if (chunkY < 0)
            return null
        return if (chunkY >= world.worldInfo.size.heightInChunks) null else world.regionsManager.getChunk(chunkX, chunkY, chunkZ)
    }

    override val allLoadedChunks: Sequence<ChunkImplementation>
        get() = world.regionsManager.regionsList.asSequence().flatMap { it.loadedChunks.asSequence() }
}