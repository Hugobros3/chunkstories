//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.generator

import xyz.chunkstories.api.voxel.VoxelFormat
import xyz.chunkstories.api.workers.Task
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldUser
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.world.chunk.ChunkLightBaker
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.storage.ChunkHolderImplementation

class TaskGenerateWorldThinSlice internal constructor(private val world: World, private val chunkX: Int, private val chunkZ: Int, private val heightmap: Heightmap) : Task(), WorldUser {

    private val holders: Array<ChunkHolderImplementation?>

    private val maxGenerationHeight: Int
    private val maxGenerationHeightInChunks: Int
    private val generator: WorldGenerator

    init {

        generator = world.generator
        maxGenerationHeight = Integer.parseInt(generator.definition.resolveProperty("maxGenerationHeight", "1024"))
        maxGenerationHeightInChunks = Math.ceil(maxGenerationHeight / 32.0).toInt()

        holders = arrayOfNulls(maxGenerationHeightInChunks)
        for (chunkY in 0 until maxGenerationHeightInChunks) {
            holders[chunkY] = world.acquireChunkHolder(this, chunkX, chunkY, chunkZ) as ChunkHolderImplementation
        }
    }

    override fun task(taskExecutor: TaskExecutor): Boolean {
        for (chunkY in 0 until maxGenerationHeightInChunks) {
            if (holders[chunkY]!!.state !is ChunkHolder.State.Generating)
                throw Exception("Trying to generate a chunk that is already generated!")
        }

        // Doing the lord's work
        val chunks = arrayOfNulls<Chunk>(holders.size)
        for (chunkY in 0 until maxGenerationHeightInChunks) {
            chunks[chunkY] = CubicChunk(holders[chunkY], chunkX, chunkY, chunkZ, null)
        }

        generator.generateWorldSlice(chunks)

        for (chunkY in 0 until maxGenerationHeightInChunks) {
            holders[chunkY]!!.eventGenerationFinishes(chunks[chunkY] as CubicChunk)
        }

        // Build the heightmap from that
        for(cy in (maxGenerationHeightInChunks - 1) downTo 0) {
            val chunk = holders[cy]!!.chunk!!
            val data = chunk.voxelDataArray

            if(data == null)
                continue

            for (x in 0..31)
                for (z in 0..31) {
                    for(i in 31 downTo 0) {
                        val y = cy * 32 + i

                        val rawData = data[x * 32 * 32 + i * 32 + z]
                        if(rawData != 0 && VoxelFormat.id(rawData) != 0) {
                            val cell: Chunk.FreshChunkCell = chunk.peek(x, y, z)
                            if (cell.voxel!!.solid || cell.voxel!!.name == "water") {
                                heightmap.setTopCell(cell)
                                break
                            }
                        }
                    }
                }
        }

        /*for (x in 0..31)
            for (z in 0..31) {
                var y = maxGenerationHeight - 1
                while (y >= 0) {
                    val rawData = holders[y / 32]!!.chunk!!.peekRaw(x, y, z)
                    if(rawData != 0 && VoxelFormat.id(rawData) != 0) {
                        val cell: Chunk.FreshChunkCell = holders[y / 32]!!.chunk!!.peek(x, y, z)
                        if (cell.voxel!!.solid || cell.voxel!!.name == "water") {
                            heightmap.setTopCell(cell)
                            break
                        }
                    }
                    y--
                }
            }*/

        // Let there be light
        for (chunkY in 0 until maxGenerationHeightInChunks) {
            (holders[chunkY]!!.chunk!!.lightBaker() as ChunkLightBaker).hackyUpdateDirect()
        }

        // Let go the world data now
        for (chunkY in 0 until maxGenerationHeightInChunks) {
            holders[chunkY]!!.unregisterUser(this)
        }

        return true
    }
}
