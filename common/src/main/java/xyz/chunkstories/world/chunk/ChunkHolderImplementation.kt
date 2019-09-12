//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.chunk

import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.util.concurrency.Fence
import xyz.chunkstories.api.world.WorldUser
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.region.Region
import xyz.chunkstories.net.packets.PacketChunkCompressedData
import xyz.chunkstories.util.concurrency.TrivialFence
import xyz.chunkstories.world.WorldTool
import xyz.chunkstories.world.io.TaskLoadChunk
import net.jpountz.lz4.LZ4Factory
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.world.WorldClientNetworkedRemote
import xyz.chunkstories.world.region.RegionImplementation
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

class ChunkHolderImplementation(override val region: RegionImplementation, override val chunkX: Int, override val chunkY: Int, override val chunkZ: Int) : ChunkHolder {
    private val uuid: Int

    // The default state of a chunk holder is to wait for the parent region to finish it's own loading
    override var state: ChunkHolder.State = ChunkHolder.State.WaitForRegionInitialLoad
    var stateHistory = mutableListOf<String>()

    fun transitionState(value: ChunkHolder.State) {
        state = value
        stateHistory.add(value.javaClass.simpleName)

        if (peopleWaiting > 0) {
            stateSemaphore.release(peopleWaiting)
            peopleWaiting = 0
        }
    }

    var peopleWaiting = 0
    val stateSemaphore = Semaphore(0)

    //No usersLock : we use the parent region usersLock & userCount
    override val users: MutableSet<WorldUser> = HashSet()
    private val usersWaitingForIntialData = HashSet<RemotePlayer>()

    /** Used by IO operations only  */
    var compressedData: ChunkCompressedData? = null

    //var loadChunkTask: IOTask? = null

    override val chunk: ChunkImplementation?
        get() = (state as? ChunkHolder.State.Available)?.chunk as? ChunkImplementation

    init {
        uuid = chunkX shl region.world.worldInfo.size.bitlengthOfVerticalChunksCoordinates or chunkY shl region.world.worldInfo.size.bitlengthOfHorizontalChunksCoordinates or chunkZ
    }

    override fun compressChunkData() {
        val chunk = this.chunk ?: return

        chunk.entitiesLock.lock()
        val compressedData = ChunkCompressedData.compressChunkData(chunk)//compressChunkData(chunk)
        chunk.entitiesLock.unlock()

        this.compressedData = compressedData
    }

    /*/** This method is called assumming the chunk is well-locked  */
    private fun compressChunkData(chunk: ChunkImplementation): CompressedData {
        val changesTakenIntoAccount = chunk.compressionUncommitedModifications.get()

        // Stage 1: Compress the actual voxel data
        val voxelCompressedData: ByteArray?
        if (!chunk.isAirChunk) {
            // Heuristic value for the size of the buffer: fixed voxel size + factor of
            // components & entities
            val uncompressedStuffBufferSize = 32 * 32 * 32 * 4// + chunk.voxelComponents.size() * 1024 +
            // chunk.localEntities.size() * 2048;
            val uncompressedStuff = MemoryUtil.memAlloc(uncompressedStuffBufferSize)

            uncompressedStuff.asIntBuffer().put(chunk.voxelDataArray)
            // uncompressedStuff.flip();

            val compressedStuff = MemoryUtil.memAlloc(uncompressedStuffBufferSize + 2048)

            val compressor = factory.fastCompressor()
            compressor.compress(uncompressedStuff, compressedStuff)

            // No longer need that buffer
            MemoryUtil.memFree(uncompressedStuff)

            // Make a Java byte[] array to put the final stuff in
            voxelCompressedData = ByteArray(compressedStuff.position())
            compressedStuff.flip()

            compressedStuff.get(voxelCompressedData)

            // No longer need that buffer either
            MemoryUtil.memFree(compressedStuff)
        } else {
            // Just use a symbolic null here
            voxelCompressedData = null
        }

        // Stage 2: Take care of the voxel components

        val baos = ByteArrayOutputStream()
        val daos = DataOutputStream(baos)

        val bbos = ByteArrayOutputStream()
        val dos = DataOutputStream(bbos)

        try {
            // For all cells that have components
            for (voxelComponents in chunk.allCellComponents.values) {

                // Write a 1 then their in-chunk index
                daos.writeByte(0x01.toByte().toInt())
                daos.writeInt(voxelComponents.index)

                // For all components in this getCell
                for ((key, value) in voxelComponents.allVoxelComponents) {
                    daos.writeUTF(key) // Write component name

                    // Push the component in the temporary buffer
                    value.push(region.handler!!, dos)

                    val bytesPushed = bbos.toByteArray()
                    bbos.reset()

                    // Write how many bytes the temporary buffer now contains
                    daos.writeShort(bytesPushed.size)

                    // Get those bytes as an array then write it in the compressed stuff
                    daos.write(bytesPushed, 0, bytesPushed.size)
                }

                daos.writeUTF("\n")
            }

            // Write the final 00, so to be clear we are done with voxel components
            daos.writeByte(0x00.toByte().toInt())

            // Since we output to a local buffer, any failure is viewed as catastrophic
        } catch (e: IOException) {
            assert(false)
        }

        // Extract the byte array from the baos
        val voxelComponentsData = baos.toByteArray()

        // Stage 3: Compress entities
        baos.reset()

        for (entity in chunk.localEntities) {
            // Don't save controllable entities
            if (!entity.traitLocation.wasRemoved() && entity.traits[TraitDontSave::class] == null) {
                EntitySerializerOld.writeEntityToStream(daos, region.handler, entity)
            }
        }
        EntitySerializerOld.writeEntityToStream(daos, region.handler, null)

        val entityData = baos.toByteArray()

        // Remove whatever modifications existed when the method started, this is for
        // avoiding concurrent modifications not being taken into account
        chunk.compressionUncommitedModifications.addAndGet(-changesTakenIntoAccount)

        return CompressedData(voxelCompressedData, voxelComponentsData, entityData)
    }*/

    override fun registerUser(user: WorldUser): Boolean {
        try {
            region.stateLock.lock()

            if (region.state is Region.State.Zombie) {
                throw Exception("You can't register an user to a Zombie region (ch state: $state")
            }

            val wasThisEmpty = users.isEmpty()
            val previousRegionUserCount = region.usersCount

            if (users.add(user)) {
                region.usersCount++

                if (previousRegionUserCount == 0)
                    region.eventUsersNotEmpty()

                if (wasThisEmpty)
                    eventUsersNotEmpty()

                globalRegisteredUsers.incrementAndGet()

                if(user is RemotePlayer) {
                    if(this.state is ChunkHolder.State.Available) {
                        user.pushPacket(PacketChunkCompressedData(chunk!!, compressedData!!))
                    } else {
                        usersWaitingForIntialData.add(user)
                    }
                }
                return true
            }

            return false
        } finally {
            region.stateLock.unlock()
        }
    }

    override fun unregisterUser(user: WorldUser): Boolean {
        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()

            /*if(state !is ChunkHolder.State.Available) {
                println("ok that's not okay ${state.javaClass.simpleName} $users")
                Thread.dumpStack()
            }*/

            val wasNotEmpty = users.isNotEmpty()
            if (users.remove(user)) {
                globalRegisteredUsers.decrementAndGet()
                region.usersCount--
            }

            if (users.isEmpty() && wasNotEmpty) {
                eventUsersEmpty()

                if (region.usersCount == 0)
                    region.eventUsersEmpty()
                return true
            }

            return false
        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }
    }

    fun eventUsersEmpty() {
        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()

            when (state) {
                ChunkHolder.State.WaitForRegionInitialLoad -> { /* legal, don't care */
                }
                ChunkHolder.State.Unloaded -> throw Exception("This doesn't make sense $stateHistory")
                is ChunkHolder.State.Generating -> { /* legal, don't care */
                }
                is ChunkHolder.State.LoadingFromServer -> {
                    transitionUnloaded()
                }
                is ChunkHolder.State.Loading -> {
                    val task = (state as ChunkHolder.State.Loading).fence as TaskLoadChunk
                    if (task.tryCancel())
                        transitionUnloaded()
                }
                is ChunkHolder.State.Available -> transitionUnloaded()
            }
        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }
    }

    fun eventUsersNotEmpty() {
        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()

            when (state) {
                ChunkHolder.State.WaitForRegionInitialLoad -> { /* legal, don't care */
                }
                ChunkHolder.State.Unloaded -> {
                    if (compressedData != null)
                        transitionLoading()
                    else if(region.state is Region.State.Generating)
                        transitionGenerating()
                    else if(region.world is WorldClientNetworkedRemote) {
                        transitionWaitingOnRemoteData()
                    } else
                        throw Exception("Broken assertion: If the chunk is unloaded, either it has to have unloaded data, or be in a yet region pending generation!")
                }
                is ChunkHolder.State.Generating -> { /* legal, don't care */
                }
                is ChunkHolder.State.Loading -> throw Exception("This doesn't make sense $stateHistory")
                is ChunkHolder.State.Available -> throw Exception("This doesn't make sense $stateHistory")
            }
        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }
    }

    fun eventRegionIsReady() {
        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()
            if (state != ChunkHolder.State.WaitForRegionInitialLoad)
                throw Exception("Illegal state change")

            if (users.isNotEmpty()) {
                if (compressedData != null)
                    transitionLoading()
                else if(region.state is Region.State.Generating)
                    transitionGenerating()
                else
                    throw Exception("Broken assertion: If the chunk is unloaded, either it has to have unloaded data, or be in a yet region pending generation!")
            } else {
                transitionUnloaded()
            }

        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }
    }

    fun eventLoadFinishes(chunk: ChunkImplementation) {
        val playersToSendDataTo: List<RemotePlayer>?

        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()

            if (state !is ChunkHolder.State.Loading) {
                logger.error("Illegal state change: Received data but wasn't in the Loading state! (was $state)")
                return
            }

            if (users.isNotEmpty()) {
                transitionAvailable(chunk)
                playersToSendDataTo = if (usersWaitingForIntialData.isNotEmpty()) usersWaitingForIntialData.toList() else null
                usersWaitingForIntialData.clear()
            } else {
                transitionUnloaded()
                playersToSendDataTo = null
            }
        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }

        if (playersToSendDataTo != null)
            for (user in playersToSendDataTo)
                user.pushPacket(PacketChunkCompressedData(chunk, compressedData!!))
    }

    fun eventGenerationFinishes(chunk: ChunkImplementation) {
        val playersToSendDataTo: List<RemotePlayer>?

        // Get the compressed data done first, to avoid keeping the lock for longer than necessary
        // val compressedData = compressChunkData(chunk)

        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()

            if (state !is ChunkHolder.State.Generating) {
                logger.error("Illegal state change: Received data but wasn't in the Generating state! (was $state)")
                return
            }

            if (users.isNotEmpty()) {
                transitionAvailable(chunk)
                compressChunkData()
                playersToSendDataTo = if (usersWaitingForIntialData.isNotEmpty()) usersWaitingForIntialData.toList() else null
                usersWaitingForIntialData.clear()
            } else {
                //TODO note this discards the generated chunk!t
                transitionUnloaded()
                playersToSendDataTo = null
            }
        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }

        if (playersToSendDataTo != null)
            for (user in playersToSendDataTo)
                user.pushPacket(PacketChunkCompressedData(chunk, compressedData!!))
    }

    private fun transitionLoading() {
        try {
            region.stateLock.lock()

            if (state !is ChunkHolder.State.WaitForRegionInitialLoad && state !is ChunkHolder.State.Unloaded)
                throw Exception("Illegal transition")

            val task = TaskLoadChunk(this)
            transitionState(ChunkHolder.State.Loading(task))
            //TODO this is a hack for working arround the lack of fibers in the task system!
            region.world.ioHandler.scheduleTask(task)
            //region.world.gameContext.tasks.scheduleTask(task)
        } finally {
            region.stateLock.unlock()
        }
    }

    private fun transitionGenerating() {
        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()

            if (state !is ChunkHolder.State.WaitForRegionInitialLoad && state !is ChunkHolder.State.Unloaded)
                throw Exception("Illegal transition")

            transitionState(ChunkHolder.State.Generating(TrivialFence()))

            if (region.world is WorldTool && !region.world.isGenerationEnabled)
                eventGenerationFinishes(ChunkImplementation(this, chunkX, chunkY, chunkZ, null))
        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }
    }

    private fun transitionWaitingOnRemoteData() {
        try {
            region.stateLock.lock()

            if (state !is ChunkHolder.State.WaitForRegionInitialLoad && state !is ChunkHolder.State.Unloaded)
                throw Exception("Illegal transition")

            transitionState(ChunkHolder.State.LoadingFromServer(TrivialFence()))
        } finally {
            region.stateLock.unlock()
        }
    }

    private fun transitionAvailable(chunk: ChunkImplementation) {
        try {
            region.stateLock.lock()
            region.loadedChunksSet.add(chunk)
            transitionState(ChunkHolder.State.Available(chunk))
        } finally {
            region.stateLock.unlock()
        }
    }

    private fun transitionUnloaded() {
        try {
            region.world.entitiesLock.writeLock().lock()
            region.stateLock.lock()
            when (state) {
                is ChunkHolder.State.Available, is ChunkHolder.State.Generating -> {
                    val chunk = (state as ChunkHolder.State.Available).chunk as ChunkImplementation

                    // Unlist it immediately
                    region.loadedChunksSet.remove(chunk)

                    // Remove the entities from this chunk from the world
                    for (entity in chunk.localEntities) {
                        // If there is no controller
                        if (entity.traits[TraitControllable::class]?.controller == null)
                            region.world.removeEntityFromList(entity)
                    }

                    // Lock it down
                    chunk.entitiesLock.lock()

                    // Compress chunk one last time before it has to go
                    compressChunkData()
                    //compressedData = compressChunkData(chunk)

                    // destroy it (returns any internal data using up ressources)
                    chunk.destroy()
                    ChunkImplementation.chunksCounter.decrementAndGet()

                    // unlock it (whoever messes with it now, his problem)
                    chunk.entitiesLock.unlock()
                }
            }

            transitionState(ChunkHolder.State.Unloaded)
        } finally {
            region.stateLock.unlock()
            region.world.entitiesLock.writeLock().unlock()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is ChunkHolderImplementation) {
            return other.uuid == uuid
        }

        return false
    }

    override fun toString(): String {
        return "ChunkHolderImplementation(region=, chunkX=$chunkX, chunkY=$chunkY, chunkZ=$chunkZ, state=${state.javaClass.simpleName}, users=${users.count()})"
    }

    fun waitUntilStateIs(stateClass: Class<out ChunkHolder.State>) = Fence {
        while (true) {
            try {
                region.stateLock.lock()
                if (state.javaClass == stateClass)
                    break

                peopleWaiting++
            } finally {
                region.stateLock.unlock()
            }
            stateSemaphore.acquireUninterruptibly()
        }
    }

    companion object {
        /** Symbolic reference indicating there is othing worth saving in this chunk, but data was generated  */
        val AIR_CHUNK_NO_DATA_SAVED = byteArrayOf()

        val globalRegisteredUsers = AtomicInteger(0)

        // LZ4 compressors & decompressors stuff
        private val factory = LZ4Factory.fastestInstance()

        internal var logger = LoggerFactory.getLogger("world.storage")
    }
}
