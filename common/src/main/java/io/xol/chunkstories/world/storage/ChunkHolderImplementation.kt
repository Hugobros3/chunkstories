//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.storage

import io.xol.chunkstories.api.entity.traits.TraitDontSave
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.server.RemotePlayer
import io.xol.chunkstories.api.workers.Task
import io.xol.chunkstories.api.world.WorldUser
import io.xol.chunkstories.api.world.chunk.ChunkHolder
import io.xol.chunkstories.entity.EntitySerializer
import io.xol.chunkstories.net.packets.PacketChunkCompressedData
import io.xol.chunkstories.util.concurrency.SafeWriteLock
import io.xol.chunkstories.world.chunk.CompressedData
import io.xol.chunkstories.world.chunk.CubicChunk
import io.xol.chunkstories.world.io.TaskLoadChunk
import net.jpountz.lz4.LZ4Factory
import org.lwjgl.system.MemoryUtil
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

class ChunkHolderImplementation(override val region: RegionImplementation, override val chunkX: Int, override val chunkY: Int, override val chunkZ: Int) : ChunkHolder {
    private val uuid: Int

    // The default state of a chunk holder is to wait for the parent region to finish it's own loading
    override var state: ChunkHolder.State = ChunkHolder.State.WaitForRegionInitialLoad
        private set
    private val stateLock = ReentrantLock()

    //No usersLock : we use the parent region usersLock & userCount
    override val users: MutableSet<WorldUser> = HashSet()
    private val usersWaitingForIntialData = HashSet<RemotePlayer>()

    // The compressed version of the chunk data
    private val compressedDataLock = SafeWriteLock()
    /** Used by IO operations only  */
    var compressedData: CompressedData? = null
        set(compressedData) {
            compressedDataLock.beginWrite()
            field = compressedData
            compressedDataLock.endWrite()
        }

    //var loadChunkTask: IOTask? = null

    override val chunk: CubicChunk?
        get() = (state as? ChunkHolder.State.Available)?.chunk as? CubicChunk

    init {
        uuid = chunkX shl region.world.worldInfo.size.bitlengthOfVerticalChunksCoordinates or chunkY shl region.world.worldInfo.size.bitlengthOfHorizontalChunksCoordinates or chunkZ
    }

    override fun compressChunkData() {
        val chunk = this.chunk ?: return

        chunk.entitiesLock.lock()
        val compressedData = compressChunkData(chunk)
        chunk.entitiesLock.unlock()

        this.compressedData = compressedData
    }

    /** This method is called assumming the chunk is well-locked  */
    private fun compressChunkData(chunk: CubicChunk): CompressedData {
        val changesTakenIntoAccount = chunk.compr_uncomittedBlockModifications.get()

        // Stage 1: Compress the actual voxel data
        val voxelCompressedData: ByteArray?
        if (!chunk.isAirChunk) {
            // Heuristic value for the size of the buffer: fixed voxel size + factor of
            // components & entities
            val uncompressedStuffBufferSize = 32 * 32 * 32 * 4// + chunk.voxelComponents.size() * 1024 +
            // chunk.localEntities.size() * 2048;
            val uncompressedStuff = MemoryUtil.memAlloc(uncompressedStuffBufferSize)

            uncompressedStuff.asIntBuffer().put(chunk.chunkVoxelData)
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

        // ByteBuffer smallBuffer = MemoryUtil.memAlloc(4096);
        // byte[] smallArray = new byte[4096];

        // ByteBufferOutputStream bbos = new ByteBufferOutputStream(smallBuffer);
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
                    // smallBuffer.flip();

                    val bytesPushed = bbos.toByteArray()
                    bbos.reset()

                    // Write how many bytes the temporary buffer now contains
                    // int bytesPushed = smallBuffer.limit();
                    daos.writeShort(bytesPushed.size)

                    // Get those bytes as an array then write it in the compressed stuff
                    // smallBuffer.getVoxelComponent(smallArray);
                    daos.write(bytesPushed, 0, bytesPushed.size)

                    // Reset the temporary buffer
                    // smallBuffer.clear();
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

        // MemoryUtil.memFree(smallBuffer);

        // Stage 3: Compress entities
        baos.reset()

        for (entity in chunk.localEntities) {

            // Don't save controllable entities
            if (!entity.traitLocation.wasRemoved() && !entity.traits.has(TraitDontSave::class.java)) {
                EntitySerializer.writeEntityToStream(daos, region.handler, entity)
            }
        }
        EntitySerializer.writeEntityToStream(daos, region.handler, null)

        val entityData = baos.toByteArray()

        // Remove whatever modifications existed when the method started, this is for
        // avoiding concurrent modifications not being taken into account
        chunk.compr_uncomittedBlockModifications.addAndGet(-changesTakenIntoAccount)

        return CompressedData(voxelCompressedData, voxelComponentsData, entityData)
    }

    private fun unloadChunk() {
        try {
            stateLock.lock()

            when(state) {
                ChunkHolder.State.WaitForRegionInitialLoad -> throw Exception("Illegal state transition: Can't unload a chunk from an early region.")
                ChunkHolder.State.Unloaded -> throw Exception("Illegal state transition: Can't unload an unloaded chunk.")
                is ChunkHolder.State.Loading -> {
                    // If we had a loading request active, try to kill it
                    val task = (state as ChunkHolder.State.Loading).fence as Task
                    if(task.tryCancel()) {
                        //If we suceeded, we can set the state back to Unloaded and return right here.
                        state = ChunkHolder.State.Unloaded
                        return
                    }
                }
            }

            assert(state is ChunkHolder.State.Available)

            val chunk = (state as ChunkHolder.State.Available).chunk as CubicChunk

            // Unlist it immediately
            region.loadedChunksSet.remove(chunk)

            // Remove the entities from this chunk from the world
            region.world.entitiesLock.writeLock().lock()
            for (entity in chunk.localEntities) {
                // If there is no controller
                if (entity.traits[TraitControllable::class]?.controller == null)
                    region.world.removeEntityFromList(entity)
            }
            region.world.entitiesLock.writeLock().unlock()

            // Lock it down
            chunk.entitiesLock.lock()

            // Compress chunk one last time before it has to go
            compressedData = compressChunkData(chunk)

            // destroy it (returns any internal data using up ressources)
            chunk.destroy()
            CubicChunk.chunksCounter.decrementAndGet()

            // unlock it (whoever messes with it now, his problem)
            chunk.entitiesLock.unlock()

            state = ChunkHolder.State.Unloaded
        } finally {
            stateLock.unlock()
        }
    }

    override fun registerUser(user: WorldUser): Boolean {
        try {
            region.usersLock.lock()

            if (users.add(user)) {
                region.usersCount++
                globalRegisteredUsers.incrementAndGet()
            }

            val chunk = this.chunk

            // If the user registering is remote, we also need to send him the data
            if (user is RemotePlayer) {
                if (chunk != null) {
                    // Chunk already loaded ? Compress and send it immediately
                    // TODO recompress chunk data each tick it's needed
                    user.pushPacket(PacketChunkCompressedData(chunk, this.compressedData))
                } else {
                    // Add him to the wait list else
                    usersWaitingForIntialData.add(user)
                }
            }

            try {
                stateLock.lock()

               if(state is ChunkHolder.State.Unloaded) {
                   val task = TaskLoadChunk(this)
                   state = ChunkHolder.State.Loading(task)
                   region.world.gameContext.tasks.scheduleTask(task)
               }
            } finally {
                stateLock.unlock()
            }

            return true
        } finally {
            region.usersLock.unlock()
        }
    }

    override fun unregisterUser(user: WorldUser): Boolean {
        try {
            region.usersLock.lock()

            if (users.remove(user)) {
                globalRegisteredUsers.decrementAndGet()
                region.usersCount--
            }

            if (users.isEmpty()) {
                unloadChunk() // Unload the chunk as soon as nobody holds on to it

                if (region.users.isEmpty())
                    region.internalUnload()
                return true
            }

            return false
        } finally {
            region.usersLock.unlock()
        }
    }

    fun whenRegionIsAvailable() {
        try {
            stateLock.lock()
            state = ChunkHolder.State.Unloaded

            try {
                region.usersLock.lock()
                if(users.isNotEmpty()) {
                    val task = TaskLoadChunk(this)
                    state = ChunkHolder.State.Loading(task)
                    region.world.gameContext.tasks.scheduleTask(task)
                }
            } finally {
                region.usersLock.unlock()
            }
        } finally {
            stateLock.unlock()
        }
    }

    fun receiveDataAndCreate(data: CompressedData?) {
        val playersToSendDataTo : List<RemotePlayer>?

        try {
            this.stateLock.lock()

            if (state !is ChunkHolder.State.Loading) {
                logger.error("Illegal state change: Received data but wasn't in the Loading state! (was $state)")
                return
            }

            //TODO check the data we are receiving is from the task we wanted

            val chunk = CubicChunk(this, chunkX, chunkY, chunkZ, data)

            state = ChunkHolder.State.Available(chunk)
            region.loadedChunksSet.add(chunk)

            try {
                region.usersLock.lock()
                playersToSendDataTo = if(usersWaitingForIntialData.isNotEmpty()) usersWaitingForIntialData.toList() else null
                usersWaitingForIntialData.clear()
            } finally {
                region.usersLock.unlock()
            }

        } finally {
            this.stateLock.unlock()
        }

        if(playersToSendDataTo != null)
        for (user in playersToSendDataTo)
            user.pushPacket(PacketChunkCompressedData(chunk, data))
    }

    override fun equals(o: Any?): Boolean {
        if (o is ChunkHolderImplementation) {
            return o.uuid == uuid
        }

        return false
    }

    companion object {
        /** Symbolic reference indicating there is othing worth saving in this chunk, but data was generated  */
        val AIR_CHUNK_NO_DATA_SAVED = byteArrayOf()

        val globalRegisteredUsers = AtomicInteger(0)

        // LZ4 compressors & decompressors stuff
        private val factory = LZ4Factory.fastestInstance()

        internal var logger = LoggerFactory.getLogger("world.chunkHolder")
    }
}
