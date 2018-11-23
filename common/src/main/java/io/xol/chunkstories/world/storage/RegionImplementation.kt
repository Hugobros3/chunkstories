//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.storage

import io.xol.chunkstories.api.entity.Entity
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.api.world.WorldUser
import io.xol.chunkstories.api.world.heightmap.Heightmap
import io.xol.chunkstories.api.world.region.Region
import io.xol.chunkstories.world.WorldImplementation
import io.xol.chunkstories.world.chunk.CubicChunk
import io.xol.chunkstories.world.io.IOTaskLoadRegion
import io.xol.chunkstories.world.io.IOTaskSaveRegion
import io.xol.chunkstories.world.region.format.CSFRegionFile
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class RegionImplementation(override val world: WorldImplementation, override val heightmap: Heightmap, override val regionX: Int, override val regionY: Int, override val regionZ: Int) : Region, WorldUser {
    val file: File?
    val handler: CSFRegionFile?

    val stateLock = ReentrantLock()
    override var state: Region.State = Region.State.Zombie
        set(value) {
            field = value
            //println("DEBUG: Region ${this@RegionImplementation} state set to $value")
        }

    /** Keeps track of the number of users of this region. Warning: this includes users of not only the region itself, but ALSO the chunks that make up this
    region. An user registered in 3 chunks for instance, will be counted three times. When this counter reaches zero, the region is unloaded.*/
    var usersCount = 0
    internal val usersSet: MutableSet<WorldUser> = HashSet()
    /** Externally exposed read-only copy of the users set */
    override var users: Set<WorldUser> = emptySet()
    //val usersLock: Lock = ReentrantLock()

    private val chunkHolders: Array<ChunkHolderImplementation>

    internal var loadedChunksSet = ConcurrentHashMap.newKeySet<CubicChunk>()
    override val loadedChunks: Sequence<CubicChunk>
        get() = loadedChunksSet.asSequence()

    override val entitiesWithinRegion: Sequence<Entity>
        get() = loadedChunks.flatMap { it.localEntities.asSequence() }

    init {
        if (regionX < 0 || regionY < 0 || regionZ < 0)
            throw RuntimeException("Regions aren't allowed negative coordinates.")

        heightmap.registerUser(this)

        // Initialize slots
        chunkHolders = Array(512) { i ->
            val cx = (i shr 6) and 0x7
            val cy = (i shr 3) and 0x7
            val cz = (i shr 0) and 0x7
            //println("cx $cx cy $cy cz $cz")
            ChunkHolderImplementation(this, (regionX shl 3) + cx, (regionY shl 3) + cy, (regionZ shl 3) + cz)
        }

        // Only the WorldMaster has a concept of files
        if (world is WorldMaster) {
            file = File(world.folderPath + "/regions/" + regionX + "." + regionY + "." + regionZ + ".csf")
            handler = CSFRegionFile.determineVersionAndCreate(this)

            if (file.exists()) {
                //assert(heightmap.state !is Heightmap.State.Generating) { "We should never have existing data if the heightmap is still generating!" }

                val task = IOTaskLoadRegion(this)
                state = Region.State.Loading(task)
                world.ioHandler.scheduleTask(task)
            } else {
                state = Region.State.Available()
                chunkHolders.forEach { it.whenRegionIsAvailable() }
            }

        } else {
            // Remote-world regions don't wait for anything to load in
            file = null
            handler = null
            state = Region.State.Available()
            chunkHolders.forEach { it.whenRegionIsAvailable() }
        }
    }

    fun whenDataLoadedCallback() {
        try {
            stateLock.lock()
            if (state !is Region.State.Loading) {
                logger.error("Illegal state transition: When accepting loaded data, region was in state $state")
                return
            }

            state = Region.State.Available()
            chunkHolders.forEach { it.whenRegionIsAvailable() }
        } finally {
            stateLock.unlock()
        }
    }

    override fun registerUser(user: WorldUser): Boolean {
        try {
            //usersLock.lock()
            stateLock.lock()

            if(state is Region.State.Zombie)
                throw Exception("Registering user in a zombie region !!!")

            if (usersSet.add(user)) {
                usersCount++
                return true
            }
            return false
        } finally {
            users = usersSet.toSet()
            //usersLock.unlock()
            stateLock.unlock()
        }
    }

    override fun unregisterUser(user: WorldUser): Boolean {
        try {
            //usersLock.lock()
            stateLock.lock()
            usersSet.remove(user)
            usersCount--

            if (usersCount == 0) {
                unload()
                return true
            }

            return false
        } finally {
            users = usersSet.toSet()
            stateLock.unlock()
            //usersLock.unlock()
        }

    }

    override fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int): CubicChunk? {
        return chunkHolders[(chunkX and 7) * 64 + (chunkY and 7) * 8 + (chunkZ and 7)].chunk
    }

    override fun getChunkHolder(chunkX: Int, chunkY: Int, chunkZ: Int): ChunkHolderImplementation {
        return chunkHolders[(chunkX and 7) * 64 + (chunkY and 7) * 8 + (chunkZ and 7)]
    }

    /** Called both when the last user logs off and when saving is done */
    internal fun unload() {
        fun remove() {
            // Remove the reference in the world to this
            world.regionsStorage.removeRegion(this)
            heightmap.unregisterUser(this)

            state = Region.State.Zombie
        }

        try {
            stateLock.lock()

            if (state is Region.State.Loading) {
                val task = (state as Region.State.Loading).fence as IOTaskLoadRegion

                // If we managed to kill the task before it executes or before it could callback whenDataLoadedCallback
                if(task.tryCancel() || state !is Region.State.Available) {
                    remove()
                    return
                } else {
                    println("failed to cancel task :(, resulting state is $state")
                }
            }

            if (state !is Region.State.Available && state !is Region.State.Saving)
                throw Exception("Illegal state transition: When unloading, region was in state $state")

            // We want to save, but only once.
            // After the first save we'll still be in Saving state and so we can transition to Zombie.
            if (world is WorldMaster && state is Region.State.Available && state !is Region.State.Saving) {
                val task = IOTaskSaveRegion(this)
                state = Region.State.Saving(task)
                world.ioHandler.scheduleTask(task)

                return // we'll actually unload later
            }

            remove()

        } finally {
            stateLock.unlock()
        }
    }

    fun whenSavingDone() {
        try {
            stateLock.lock()
            if (usersCount == 0)
                unload()
            else {
                try {
                    //stateLock.lock()
                    if (state is Region.State.Saving)
                        state = Region.State.Available()
                } finally {
                    //stateLock.unlock()
                }
            }

        } finally {
            stateLock.unlock()
        }
    }

    override fun toString(): String {
        return ("[Region rx:$regionX ry:$regionY rz:$regionZ state:${state.javaClass.simpleName} chunks:${loadedChunks.count()} entities:${entitiesWithinRegion.count()}]")
    }

    fun compressAll() {
        for (a in 0..7)
            for (b in 0..7)
                for (c in 0..7)
                    chunkHolders[a * 64 + b * 8 + c].compressChunkData()
    }

    fun compressChangedChunks() {
        for (a in 0..7)
            for (b in 0..7)
                for (c in 0..7) {
                    val chunk = chunkHolders[a * 64 + b * 8 + c].chunk
                    if (chunk != null) {

                        if (chunk.compressionUncommitedModifications.get() > 0)
                            chunkHolders[a * 64 + b * 8 + c].compressChunkData()
                    }
                }
    }

    companion object {
        internal var logger = LoggerFactory.getLogger("world.storage")
    }
}
