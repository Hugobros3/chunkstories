//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.chunk

import java.util.concurrent.locks.ReentrantLock

import com.carrotsearch.hppc.IntArrayDeque
import com.carrotsearch.hppc.IntDeque

import xyz.chunkstories.api.voxel.VoxelFormat
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.chunk.ChunkLightUpdater
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.world.WorldTool
import xyz.chunkstories.world.cell.ScratchCell
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

//TODO use custom propagation for ALL propagation functions & cleanup this whole darn mess
/** Responsible for propagating voxel volumetric light  */
class ChunkLightBaker(internal val chunk: CubicChunk) : AutoRebuildingProperty(chunk.world.gameContext, true), ChunkLightUpdater {
    internal val world = chunk.world!!
    internal val chunkX: Int = chunk.chunkX
    internal val chunkY: Int = chunk.chunkY
    internal val chunkZ: Int = chunk.chunkZ

    internal val lightDataLock = ReentrantLock()

    override fun createTask(updatesToConsider: Int): UpdateTask = TaskLightChunk(this, updatesToConsider, true)

    /** Hack until we move to fibers/coroutines: immediately do the lighting baking ( to avoid worker gridlocking ) */
    fun hackyUpdateDirect() {
        val task = TaskLightChunk(this, 0, true)
        task.findAndSpreadLight()
    }

    /** Called when a voxel is changed  */
    fun computeLightSpread(bx: Int, by: Int, bz: Int, dataBefore: Int, data: Int) {
        try {
            lightDataLock.lock()

            val sunLightBefore = VoxelFormat.sunlight(dataBefore)
            val blockLightBefore = VoxelFormat.blocklight(dataBefore)

            var sunLightAfter = VoxelFormat.sunlight(data)
            val blockLightAfter = VoxelFormat.blocklight(data)

            val csh = world.regionsSummariesHolder.getHeightAtWorldCoordinates(bx + chunkX * 32, bz + chunkZ * 32)
            val block_height = by + chunkY * 32

            // If the block is at or above (never) the topmost tile it's sunlit
            if (block_height >= csh)
                sunLightAfter = 15

            val blockSourcesRemoval = tl_blockSourcesRemoval.get()
            val sunSourcesRemoval = tl_sunSourcesRemoval.get()
            val blockSources = tl_blockSources.get()
            val sunSources = tl_sunSources.get()

            blockSourcesRemoval.clear()
            sunSourcesRemoval.clear()
            blockSources.clear()
            sunSources.clear()

            blockSourcesRemoval.addLast(bx)
            blockSourcesRemoval.addLast(by)
            blockSourcesRemoval.addLast(bz)
            blockSourcesRemoval.addLast(blockLightBefore)

            sunSourcesRemoval.addLast(bx)
            sunSourcesRemoval.addLast(by)
            sunSourcesRemoval.addLast(bz)
            sunSourcesRemoval.addLast(sunLightBefore)

            propagateLightRemovalBeyondChunks(blockSources, sunSources, blockSourcesRemoval, sunSourcesRemoval)

            // Add light sources if relevant
            if (sunLightAfter > 0) {
                sunSources.addLast(bx)
                sunSources.addLast(by)
                sunSources.addLast(bz)
            }
            if (blockLightAfter > 0) {
                blockSources.addLast(bx)
                blockSources.addLast(by)
                blockSources.addLast(bz)
            }

            // Propagate remaining light
            this.propagateLightningBeyondChunk(blockSources, sunSources)
        } finally {
            lightDataLock.unlock()
        }
    }

    inner class TaskLightChunk(baker: ChunkLightBaker, private val updatesToCommit: Int, private val considerAdjacentChunks: Boolean) : AutoRebuildingProperty.UpdateTask(baker, updatesToCommit) {
        private val leftChunk: CubicChunk?
        private val rightChunk: CubicChunk?
        private val topChunk: CubicChunk?
        private val bottomChunk: CubicChunk?
        private val frontChunk: CubicChunk?
        private val backChunk: CubicChunk?

        init {
            // Checks if the adjacent chunks are done loading
            topChunk = world.getChunk(chunkX, chunkY + 1, chunkZ)
            bottomChunk = world.getChunk(chunkX, chunkY - 1, chunkZ)
            frontChunk = world.getChunk(chunkX, chunkY, chunkZ + 1)
            backChunk = world.getChunk(chunkX, chunkY, chunkZ - 1)
            leftChunk = world.getChunk(chunkX - 1, chunkY, chunkZ)
            rightChunk = world.getChunk(chunkX + 1, chunkY, chunkZ)
        }

        override fun update(taskExecutor: TaskExecutor): Boolean {
            if (updatesToCommit == 0)
                return true

            // Actual computation takes place here
            findAndSpreadLight()

            return true
        }

        internal fun findAndSpreadLight(): Int {
            try {
                lightDataLock.lock()
                // Checks first if chunk contains blocks
                if (chunk.voxelDataArray == null)
                    return 0 // Nothing to do

                // Lock the chunk & grab 2 queues
                val blockSources = tl_blockSources.get()
                val sunSources = tl_sunSources.get()

                // Reset any remnant data
                blockSources.clear()
                sunSources.clear()

                // Find our own light sources, add them
                findLightSources(blockSources, sunSources)

                var modifications = 0

                // Load nearby chunks and check if they contain bright spots we haven't
                // accounted for yet
                if (considerAdjacentChunks)
                    modifications += propagateLightFromBorders(blockSources, sunSources)

                // Propagates the light
                modifications += propagateLightSources(blockSources, sunSources, considerAdjacentChunks)

                // Have some blocks changed ?
                if (modifications > 0)
                    chunk.mesh.requestUpdate()

                return modifications
            } finally {
                lightDataLock.unlock()
            }
        }

        private fun propagateLightSources(blockSources: IntDeque, sunSources: IntDeque, propagateToAdjacentChunks: Boolean): Int {
            var modifiedBlocks = 0

            /*val leftChunk: CubicChunk? = world.getChunk(chunkX - 1, chunkY, chunkZ)
            val rightChunk: CubicChunk? = world.getChunk(chunkX + 1, chunkY, chunkZ)
            val topChunk: CubicChunk? = world.getChunk(chunkX, chunkY + 1, chunkZ)
            val bottomChunk: CubicChunk? = world.getChunk(chunkX, chunkY - 1, chunkZ)
            val frontChunk: CubicChunk? = world.getChunk(chunkX, chunkY, chunkZ + 1)
            val backChunk: CubicChunk? = world.getChunk(chunkX, chunkY, chunkZ - 1)*/

            // Don't spam the requeue requests
            var checkTopBleeding = propagateToAdjacentChunks && topChunk != null
            var checkBottomBleeding = propagateToAdjacentChunks && bottomChunk != null
            var checkFrontBleeding = propagateToAdjacentChunks && frontChunk != null
            var checkBackBleeding = propagateToAdjacentChunks && backChunk != null
            var checkLeftBleeding = propagateToAdjacentChunks && leftChunk != null
            var checkRightBleeding = propagateToAdjacentChunks && rightChunk != null

            var requestTop = false
            var requestBot = false
            var requestFront = false
            var requestBack = false
            var requestLeft = false
            var requestRight = false

            val cell = ScratchCell(world)
            val adj = ScratchCell(world)
            while (blockSources.size() > 0) {
                val z = blockSources.removeLast()
                val y = blockSources.removeLast()
                val x = blockSources.removeLast()

                peek(x, y, z, cell)
                var cellLightLevel = cell.blocklight

                if (cell.voxel.opaque)
                    cellLightLevel = cell.voxel.getEmittedLightLevel(cell)

                if (cellLightLevel > 1) {
                    if (x < 31) {
                        peek(x + 1, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.LEFT) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            adj.blocklight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            blockSources.addLast(x + 1)
                            blockSources.addLast(y)
                            blockSources.addLast(z)
                        }
                    } else if (checkRightBleeding) {
                        peek(32, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.LEFT) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            requestRight = true
                            checkRightBleeding = false
                        }
                    }
                    if (x > 0) {
                        peek(x - 1, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.RIGHT) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            adj.blocklight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            blockSources.addLast(x - 1)
                            blockSources.addLast(y)
                            blockSources.addLast(z)
                        }
                    } else if (checkLeftBleeding) {
                        peek(-1, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.RIGHT) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            requestLeft = true
                            checkLeftBleeding = false
                        }
                    }

                    if (z < 31) {
                        peek(x, y, z + 1, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BACK) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            adj.blocklight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y)
                            blockSources.addLast(z + 1)
                        }
                    } else if (checkFrontBleeding) {
                        peek(x, y, 32, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BACK) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            requestFront = true
                            checkFrontBleeding = false
                        }
                    }
                    if (z > 0) {
                        peek(x, y, z - 1, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.FRONT) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            adj.blocklight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y)
                            blockSources.addLast(z - 1)
                        }
                    } else if (checkBackBleeding) {
                        peek(x, y, -1, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.FRONT) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            requestBack = true
                            checkBackBleeding = false
                        }
                    }

                    if (y < 31) {
                        peek(x, y + 1, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BOTTOM) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            adj.blocklight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y + 1)
                            blockSources.addLast(z)
                        }
                    } else if (checkTopBleeding) {
                        peek(x, 32, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BOTTOM) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            requestTop = true
                            checkTopBleeding = false
                        }
                    }

                    if (y > 0) {
                        peek(x, y - 1, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.TOP) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            adj.blocklight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y - 1)
                            blockSources.addLast(z)
                        }
                    } else if (checkBottomBleeding) {
                        peek(x, -1, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.TOP) + 1)
                        if (adj.blocklight < fadedLightLevel) {
                            requestBot = true
                            checkBottomBleeding = false
                        }
                    }
                }
            }

            while (sunSources.size() > 0) {
                val z = sunSources.removeLast()
                val y = sunSources.removeLast()
                val x = sunSources.removeLast()

                peek(x, y, z, cell)
                var cellLightLevel = cell.sunlight

                if (cell.voxel.opaque)
                    cellLightLevel = cell.voxel.getEmittedLightLevel(cell)

                if (cellLightLevel > 1) {
                    if (x < 31) {
                        peek(x + 1, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.LEFT) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            adj.sunlight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            sunSources.addLast(x + 1)
                            sunSources.addLast(y)
                            sunSources.addLast(z)
                        }
                    } else if (checkRightBleeding) {
                        peek(32, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.LEFT) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            requestRight = true
                            checkRightBleeding = false
                        }
                    }
                    if (x > 0) {
                        peek(x - 1, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.RIGHT) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            adj.sunlight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            sunSources.addLast(x - 1)
                            sunSources.addLast(y)
                            sunSources.addLast(z)
                        }
                    } else if (checkLeftBleeding) {
                        peek(-1, y, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.RIGHT) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            requestLeft = true
                            checkLeftBleeding = false
                        }
                    }

                    if (z < 31) {
                        peek(x, y, z + 1, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BACK) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            adj.sunlight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y)
                            sunSources.addLast(z + 1)
                        }
                    } else if (checkFrontBleeding) {
                        peek(x, y, 32, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BACK) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            requestFront = true
                            checkFrontBleeding = false
                        }
                    }
                    if (z > 0) {
                        peek(x, y, z - 1, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.FRONT) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            adj.sunlight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y)
                            sunSources.addLast(z - 1)
                        }
                    } else if (checkBackBleeding) {
                        peek(x, y, -1, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.FRONT) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            requestBack = true
                            checkBackBleeding = false
                        }
                    }

                    if (y < 31) {
                        peek(x, y + 1, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BOTTOM) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            adj.sunlight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y + 1)
                            sunSources.addLast(z)
                        }
                    } else if (checkTopBleeding) {
                        peek(x, 32, z, adj)
                        val fadedLightLevel = cellLightLevel - (adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.BOTTOM) + 1)
                        if (adj.sunlight < fadedLightLevel) {
                            requestTop = true
                            checkTopBleeding = false
                        }
                    }

                    // Special case! This is the bottom computation for light spread, light doesn't
                    // fade when traveling backwards so we do not decrement fadedLightLevel !
                    if (y > 0) {
                        peek(x, y - 1, z, adj)
                        val fadedLightLevel = cellLightLevel - adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.TOP)
                        if (adj.sunlight < fadedLightLevel) {
                            adj.sunlight = fadedLightLevel
                            poke(adj)
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y - 1)
                            sunSources.addLast(z)
                        }
                    } else if (checkBottomBleeding) {
                        peek(x, -1, z, adj)
                        val fadedLightLevel = cellLightLevel - adj.voxel.getLightLevelModifier(adj, cell, VoxelSide.TOP)
                        if (adj.sunlight < fadedLightLevel) {
                            requestBot = true
                            checkBottomBleeding = false
                        }
                    }
                }
            }

            if (requestTop)
                topChunk?.lightBaker?.requestUpdate()
            if (requestBot)
                bottomChunk?.lightBaker?.requestUpdate()
            if (requestLeft)
                leftChunk?.lightBaker?.requestUpdate()
            if (requestRight)
                rightChunk?.lightBaker?.requestUpdate()
            if (requestBack)
                backChunk?.lightBaker?.requestUpdate()
            if (requestFront)
                frontChunk?.lightBaker?.requestUpdate()

            return modifiedBlocks
        }

        private fun findLightSources(blockSources: IntDeque, sunSources: IntDeque) {
            val cell = ScratchCell(world)
            for (x in 0..31)
                for (z in 0..31) {
                    var y = 31
                    var hitGroundYet = false

                    val csh = world.regionsSummariesHolder.getHeightAtWorldCoordinates(chunkX * 32 + x, chunkZ * 32 + z)
                    while (y >= 0) {
                        peek(x, y, z, cell)
                        val ll = cell.voxel.getEmittedLightLevel(cell)

                        if (ll > 0) {
                            cell.blocklight = ll
                            blockSources.addLast(x)
                            blockSources.addLast(y)
                            blockSources.addLast(z)
                        }

                        if (!hitGroundYet && csh != Heightmap.NO_DATA) {
                            if (chunkY * 32 + y >= csh) {
                                if (chunkY * 32 + y <= csh ||
                                        !world.contentTranslator.getVoxelForId(VoxelFormat.id(chunk.voxelDataArray!![x * 1024 + y * 32 + z]))!!.isAir())
                                    hitGroundYet = true
                                else {
                                    cell.sunlight = 15
                                    sunSources.addLast(x)
                                    sunSources.addLast(y)
                                    sunSources.addLast(z)
                                }
                            }
                        }

                        poke(cell)
                        y--
                    }
                }
        }

        private fun propagateLightFromBorders(blockSources: IntDeque, sunSources: IntDeque): Int {
            val cell = ScratchCell(world)
            val adj = ScratchCell(world)

            var mods = 0

            if (rightChunk != null) {
                for (z in 0..31)
                    for (y in 0..31) {
                        peek(32, y, z, adj)
                        peek(31, y, z, cell)

                        val modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSide.RIGHT) + 1
                        if (adj.blocklight - modifier > cell.blocklight) {
                            cell.blocklight = adj.blocklight - modifier
                            poke(cell)
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlight - modifier > cell.sunlight) {
                            cell.sunlight = adj.sunlight - modifier
                            mods++
                            poke(cell)
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }
            if (leftChunk != null) {
                for (z in 0..31)
                    for (y in 0..31) {
                        peek(-1, y, z, adj)
                        peek(0, y, z, cell)

                        val modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSide.LEFT) + 1
                        if (adj.blocklight - modifier > cell.blocklight) {
                            cell.blocklight = adj.blocklight - modifier
                            poke(cell)
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlight - modifier > cell.sunlight) {
                            cell.sunlight = adj.sunlight - modifier
                            mods++
                            poke(cell)
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }
            if (topChunk != null && !topChunk.isAirChunk) {
                for (z in 0..31)
                    for (x in 0..31) {
                        peek(x, 32, z, adj)
                        peek(x, 31, z, cell)

                        var modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSide.TOP) + 1
                        if (adj.blocklight - modifier > cell.blocklight) {
                            cell.blocklight = adj.blocklight - modifier
                            poke(cell)
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        modifier -= 1 // sunlight doesn't dim travelling downwards
                        if (adj.sunlight - modifier > cell.sunlight) {
                            cell.sunlight = adj.sunlight - modifier
                            mods++
                            poke(cell)
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            } else {
                for (x in 0..31)
                    for (z in 0..31) {
                        peek(x, 32, z, adj)
                        peek(x, 31, z, cell)

                        val modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSide.TOP)
                        if (adj.sunlight - modifier > cell.sunlight) {
                            cell.sunlight = adj.sunlight - modifier
                            poke(cell)
                            mods++
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }
            if (bottomChunk != null) {
                for (z in 0..31)
                    for (x in 0..31) {
                        peek(x, -1, z, adj)
                        peek(x, 0, z, cell)

                        val modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSide.BOTTOM) + 1
                        if (adj.blocklight - modifier > cell.blocklight) {
                            cell.blocklight = adj.blocklight - modifier
                            poke(cell)
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlight - modifier > cell.sunlight) {
                            cell.sunlight = adj.sunlight - modifier
                            mods++
                            poke(cell)
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }
            // cc = world.getChunk(chunkX, chunkY, chunkZ + 1);
            if (frontChunk != null) {
                for (y in 0..31)
                    for (x in 0..31) {
                        peek(x, y, 32, adj)
                        peek(x, y, 31, cell)

                        val modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSide.FRONT) + 1
                        if (adj.blocklight - modifier > cell.blocklight) {
                            cell.blocklight = adj.blocklight - modifier
                            poke(cell)
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlight - modifier > cell.sunlight) {
                            cell.sunlight = adj.sunlight - modifier
                            mods++
                            poke(cell)
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }
            // cc = world.getChunk(chunkX, chunkY, chunkZ - 1);
            if (backChunk != null) {
                for (y in 0..31)
                    for (x in 0..31) {
                        peek(x, y, -1, adj)
                        peek(x, y, 0, cell)

                        val modifier = cell.voxel.getLightLevelModifier(cell, adj, VoxelSide.BACK) + 1
                        if (adj.blocklight - modifier > cell.blocklight) {
                            cell.blocklight = adj.blocklight - modifier
                            poke(cell)
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlight - modifier > cell.sunlight) {
                            cell.sunlight = adj.sunlight - modifier
                            mods++
                            poke(cell)
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }

            return mods
        }
    }

    // TODO use getLightLevelModifier
    private fun propagateLightRemovalBeyondChunks(blockSources: IntDeque, sunSources: IntDeque, blockSourcesRemoval: IntDeque, sunSourcesRemoval: IntDeque) {
        val bounds = 64

        fun getSunLight(x: Int, y: Int, z: Int): Int =
                if (x in 0..31 && y in 0..31 && z in 0..31)
                    VoxelFormat.sunlight(chunk.peekRaw(x, y, z))
                else
                    VoxelFormat.sunlight(this.peekRawFast(x, y, z))

        fun setSunLight(x: Int, y: Int, z: Int, level: Int) =
                if (x in 0..31 && y in 0..31 && z in 0..31)
                    chunk.pokeRawSilently(x, y, z, VoxelFormat.changeSunlight(chunk.peekRaw(x, y, z), level))
                else
                    this.pokeRawFast(x, y, z, VoxelFormat.changeSunlight(this.peekRawFast(x, y, z), level))

        while (sunSourcesRemoval.size() > 0) {
            val sunLightLevel = sunSourcesRemoval.removeLast()
            val z = sunSourcesRemoval.removeLast()
            val y = sunSourcesRemoval.removeLast()
            val x = sunSourcesRemoval.removeLast()

            // X Axis
            if (x > -bounds) {
                val neighborSunLightLevel = getSunLight(x - 1, y, z)
                if (neighborSunLightLevel in 1..(sunLightLevel - 1)) {
                    setSunLight(x - 1, y, z, 0)
                    sunSourcesRemoval.addLast(x - 1)
                    sunSourcesRemoval.addLast(y)
                    sunSourcesRemoval.addLast(z)
                    sunSourcesRemoval.addLast(neighborSunLightLevel)
                } else if (neighborSunLightLevel >= sunLightLevel) {
                    sunSources.addLast(x - 1)
                    sunSources.addLast(y)
                    sunSources.addLast(z)
                }
            }
            if (x < bounds) {
                val neighborSunLightLevel = getSunLight(x + 1, y, z)
                if (neighborSunLightLevel in 1..(sunLightLevel - 1)) {
                    setSunLight(x + 1, y, z, 0)
                    sunSourcesRemoval.addLast(x + 1)
                    sunSourcesRemoval.addLast(y)
                    sunSourcesRemoval.addLast(z)
                    sunSourcesRemoval.addLast(neighborSunLightLevel)
                } else if (neighborSunLightLevel >= sunLightLevel) {
                    sunSources.addLast(x + 1)
                    sunSources.addLast(y)
                    sunSources.addLast(z)
                }
            }
            // Y axis
            if (y > -bounds) {
                val neighborSunLightLevel = getSunLight(x, y - 1, z)
                if (neighborSunLightLevel in 1..sunLightLevel) {
                    setSunLight(x, y - 1, z, 0)
                    sunSourcesRemoval.addLast(x)
                    sunSourcesRemoval.addLast(y - 1)
                    sunSourcesRemoval.addLast(z)
                    sunSourcesRemoval.addLast(neighborSunLightLevel)
                } else if (neighborSunLightLevel >= sunLightLevel) {
                    sunSources.addLast(x)
                    sunSources.addLast(y - 1)
                    sunSources.addLast(z)
                }
            }
            if (y < bounds) {
                val neighborSunLightLevel = getSunLight(x, y + 1, z)

                if (neighborSunLightLevel in 1..(sunLightLevel - 1)) {
                    setSunLight(x, y + 1, z, 0)
                    sunSourcesRemoval.addLast(x)
                    sunSourcesRemoval.addLast(y + 1)
                    sunSourcesRemoval.addLast(z)
                    sunSourcesRemoval.addLast(neighborSunLightLevel)
                } else if (neighborSunLightLevel >= sunLightLevel) {
                    sunSources.addLast(x)
                    sunSources.addLast(y + 1)
                    sunSources.addLast(z)
                }
            }
            // Z Axis
            if (z > -bounds) {
                val neighborSunLightLevel = getSunLight(x, y, z - 1)
                if (neighborSunLightLevel in 1..(sunLightLevel - 1)) {
                    setSunLight(x, y, z - 1, 0)
                    sunSourcesRemoval.addLast(x)
                    sunSourcesRemoval.addLast(y)
                    sunSourcesRemoval.addLast(z - 1)
                    sunSourcesRemoval.addLast(neighborSunLightLevel)
                } else if (neighborSunLightLevel >= sunLightLevel) {
                    sunSources.addLast(x)
                    sunSources.addLast(y)
                    sunSources.addLast(z - 1)
                }
            }
            if (z < bounds) {
                val neighborSunLightLevel = getSunLight(x, y, z + 1)
                if (neighborSunLightLevel in 1..(sunLightLevel - 1))
                // TODO wrong!
                {
                    setSunLight(x, y, z + 1, 0)
                    sunSourcesRemoval.addLast(x)
                    sunSourcesRemoval.addLast(y)
                    sunSourcesRemoval.addLast(z + 1)
                    sunSourcesRemoval.addLast(neighborSunLightLevel)
                } else if (neighborSunLightLevel >= sunLightLevel) {
                    sunSources.addLast(x)
                    sunSources.addLast(y)
                    sunSources.addLast(z + 1)
                }
            }
        }

        fun getBlockLight(x: Int, y: Int, z: Int): Int =
                if (x in 0..31 && y in 0..31 && z in 0..31)
                    VoxelFormat.blocklight(chunk.peekRaw(x, y, z))
                else
                    VoxelFormat.blocklight(this.peekRawFast(x, y, z))


        fun setBlockLight(x: Int, y: Int, z: Int, level: Int) =
                if (x in 0..31 && y in 0..31 && z in 0..31)
                    chunk.pokeRawSilently(x, y, z, VoxelFormat.changeBlocklight(chunk.peekRaw(x, y, z), level))
                else
                    this.pokeRawFast(x, y, z, VoxelFormat.changeBlocklight(this.peekRawFast(x, y, z), level))

        while (blockSourcesRemoval.size() > 0) {
            val blockLightLevel = blockSourcesRemoval.removeLast()
            val z = blockSourcesRemoval.removeLast()
            val y = blockSourcesRemoval.removeLast()
            val x = blockSourcesRemoval.removeLast()

            // X Axis
            if (x > -bounds) {
                val neighborBlockLightLevel = getBlockLight(x - 1, y, z)
                if (neighborBlockLightLevel in 1..(blockLightLevel - 1)) {
                    setBlockLight(x - 1, y, z, 0)
                    blockSourcesRemoval.addLast(x - 1)
                    blockSourcesRemoval.addLast(y)
                    blockSourcesRemoval.addLast(z)
                    blockSourcesRemoval.addLast(neighborBlockLightLevel)
                } else if (neighborBlockLightLevel >= blockLightLevel) {
                    blockSources.addLast(x - 1)
                    blockSources.addLast(y)
                    blockSources.addLast(z)
                }
            }
            if (x < bounds) {
                val neighborBlockLightLevel = getBlockLight(x + 1, y, z)
                if (neighborBlockLightLevel in 1..(blockLightLevel - 1)) {
                    setBlockLight(x + 1, y, z, 0)
                    blockSourcesRemoval.addLast(x + 1)
                    blockSourcesRemoval.addLast(y)
                    blockSourcesRemoval.addLast(z)
                    blockSourcesRemoval.addLast(neighborBlockLightLevel)
                } else if (neighborBlockLightLevel >= blockLightLevel) {
                    blockSources.addLast(x + 1)
                    blockSources.addLast(y)
                    blockSources.addLast(z)
                }
            }
            // Y axis
            if (y > -bounds) {
                val neighborBlockLightLevel = getBlockLight(x, y - 1, z)
                if (neighborBlockLightLevel in 1..(blockLightLevel - 1)) {
                    setBlockLight(x, y - 1, z, 0)
                    blockSourcesRemoval.addLast(x)
                    blockSourcesRemoval.addLast(y - 1)
                    blockSourcesRemoval.addLast(z)
                    blockSourcesRemoval.addLast(neighborBlockLightLevel)
                } else if (neighborBlockLightLevel >= blockLightLevel) {
                    blockSources.addLast(x)
                    blockSources.addLast(y - 1)
                    blockSources.addLast(z)
                }
            }
            if (y < bounds) {
                val neighborBlockLightLevel = getBlockLight(x, y + 1, z)
                if (neighborBlockLightLevel in 1..(blockLightLevel - 1)) {
                    setBlockLight(x, y + 1, z, 0)
                    blockSourcesRemoval.addLast(x)
                    blockSourcesRemoval.addLast(y + 1)
                    blockSourcesRemoval.addLast(z)
                    blockSourcesRemoval.addLast(neighborBlockLightLevel)
                } else if (neighborBlockLightLevel >= blockLightLevel) {
                    blockSources.addLast(x)
                    blockSources.addLast(y + 1)
                    blockSources.addLast(z)
                }
            }
            // Z Axis
            if (z > -bounds) {
                val neighborBlockLightLevel = getBlockLight(x, y, z - 1)
                if (neighborBlockLightLevel in 1..(blockLightLevel - 1)) {
                    setBlockLight(x, y, z - 1, 0)
                    blockSourcesRemoval.addLast(x)
                    blockSourcesRemoval.addLast(y)
                    blockSourcesRemoval.addLast(z - 1)
                    blockSourcesRemoval.addLast(neighborBlockLightLevel)
                } else if (neighborBlockLightLevel >= blockLightLevel) {
                    blockSources.addLast(x)
                    blockSources.addLast(y)
                    blockSources.addLast(z - 1)
                }
            }
            if (z < bounds) {
                val neighborBlockLightLevel = getBlockLight(x, y, z + 1)
                if (neighborBlockLightLevel in 1..(blockLightLevel - 1)) {
                    setBlockLight(x, y, z + 1, 0)
                    blockSourcesRemoval.addLast(x)
                    blockSourcesRemoval.addLast(y)
                    blockSourcesRemoval.addLast(z + 1)
                    blockSourcesRemoval.addLast(neighborBlockLightLevel)
                } else if (neighborBlockLightLevel >= blockLightLevel) {
                    blockSources.addLast(x)
                    blockSources.addLast(y)
                    blockSources.addLast(z + 1)
                }
            }
        }
    }

    // TODO use getLightLevelModifier
    private fun propagateLightningBeyondChunk(blockSources: IntDeque, sunSources: IntDeque): Int {
        var modifiedBlocks = 0
        val bounds = 64

        val cell = ScratchCell(world)
        val sideCell = ScratchCell(world)
        while (blockSources.size() > 0) {
            val z = blockSources.removeLast()
            val y = blockSources.removeLast()
            val x = blockSources.removeLast()
            peek(x, y, z, cell)
            var ll = cell.blocklight

            if (cell.voxel.opaque)
                ll = cell.voxel.getEmittedLightLevel(cell)

            if (ll > 1) {
                // X-propagation
                if (x < bounds) {
                    val adj = this.peekRawFast(x + 1, y, z)
                    if (!world.contentTranslator.getVoxelForId(adj and 0xFFFF)!!.opaque && adj and blocklightMask shr blockBitshift < ll - 1) {
                        this.pokeRawFast(x + 1, y, z, adj and blockAntiMask or (ll - 1 shl blockBitshift))
                        modifiedBlocks++
                        blockSources.addLast(x + 1)
                        blockSources.addLast(y)
                        blockSources.addLast(z)
                    }
                }
                if (x > -bounds) {
                    val adj = this.peekRawFast(x - 1, y, z)
                    if (!world.contentTranslator.getVoxelForId(adj and 0xFFFF)!!.opaque && adj and blocklightMask shr blockBitshift < ll - 1) {
                        this.pokeRawFast(x - 1, y, z, adj and blockAntiMask or (ll - 1 shl blockBitshift))
                        modifiedBlocks++
                        blockSources.addLast(x - 1)
                        blockSources.addLast(y)
                        blockSources.addLast(z)
                    }
                }
                // Z-propagation
                if (z < bounds) {
                    val adj = this.peekRawFast(x, y, z + 1)
                    if (!world.contentTranslator.getVoxelForId(adj and 0xFFFF)!!.opaque && adj and blocklightMask shr blockBitshift < ll - 1) {
                        this.pokeRawFast(x, y, z + 1, adj and blockAntiMask or (ll - 1 shl blockBitshift))
                        modifiedBlocks++
                        blockSources.addLast(x)
                        blockSources.addLast(y)
                        blockSources.addLast(z + 1)
                    }
                }
                if (z > -bounds) {
                    val adj = this.peekRawFast(x, y, z - 1)
                    if (!world.contentTranslator.getVoxelForId(adj and 0xFFFF)!!.opaque && adj and blocklightMask shr blockBitshift < ll - 1) {
                        this.pokeRawFast(x, y, z - 1, adj and blockAntiMask or (ll - 1 shl blockBitshift))
                        modifiedBlocks++
                        blockSources.addLast(x)
                        blockSources.addLast(y)
                        blockSources.addLast(z - 1)
                    }
                }
                // Y-propagation
                if (y < bounds)
                // y = 254+1
                {
                    val adj = this.peekRawFast(x, y + 1, z)
                    if (!world.contentTranslator.getVoxelForId(adj and 0xFFFF)!!.opaque && adj and blocklightMask shr blockBitshift < ll - 1) {
                        this.pokeRawFast(x, y + 1, z, adj and blockAntiMask or (ll - 1 shl blockBitshift))
                        modifiedBlocks++
                        blockSources.addLast(x)
                        blockSources.addLast(y + 1)
                        blockSources.addLast(z)
                    }
                }
                if (y > -bounds) {
                    val adj = this.peekRawFast(x, y - 1, z)
                    if (!world.contentTranslator.getVoxelForId(adj and 0xFFFF)!!.opaque && adj and blocklightMask shr blockBitshift < ll - 1) {
                        this.pokeRawFast(x, y - 1, z, adj and blockAntiMask or (ll - 1 shl blockBitshift))
                        modifiedBlocks++
                        blockSources.addLast(x)
                        blockSources.addLast(y - 1)
                        blockSources.addLast(z)
                    }
                }
            }
        }
        // Sunlight propagation
        while (sunSources.size() > 0) {
            val z = sunSources.removeLast()
            val y = sunSources.removeLast()
            val x = sunSources.removeLast()
            peek(x, y, z, cell)
            var ll = cell.sunlight

            if (cell.voxel.opaque)
                ll = 0

            if (ll > 1) {
                // X-propagation
                if (x < bounds) {
                    peek(x + 1, y, z, sideCell)
                    val llRight = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSide.LEFT)
                    if (!sideCell.voxel.opaque && sideCell.sunlight < llRight - 1) {
                        sideCell.sunlight = llRight - 1
                        poke(sideCell)
                        modifiedBlocks++
                        sunSources.addLast(x + 1)
                        sunSources.addLast(y)
                        sunSources.addLast(z)
                    }
                }
                if (x > -bounds) {
                    peek(x - 1, y, z, sideCell)
                    val llLeft = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSide.RIGHT)
                    if (!sideCell.voxel.opaque && sideCell.sunlight < llLeft - 1) {
                        sideCell.sunlight = llLeft - 1
                        poke(sideCell)
                        modifiedBlocks++
                        sunSources.addLast(x - 1)
                        sunSources.addLast(y)
                        sunSources.addLast(z)
                    }
                }
                // Z-propagation
                if (z < bounds) {
                    peek(x, y, z + 1, sideCell)
                    val llFront = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSide.BACK)
                    if (!sideCell.voxel.opaque && sideCell.sunlight < llFront - 1) {
                        sideCell.sunlight = llFront - 1
                        poke(sideCell)
                        modifiedBlocks++
                        sunSources.addLast(x)
                        sunSources.addLast(y)
                        sunSources.addLast(z + 1)
                    }
                }
                if (z > -bounds) {
                    peek(x, y, z - 1, sideCell)
                    val llBack = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSide.FRONT)
                    if (!sideCell.voxel.opaque && sideCell.sunlight < llBack - 1) {
                        sideCell.sunlight = llBack - 1
                        poke(sideCell)
                        modifiedBlocks++
                        sunSources.addLast(x)
                        sunSources.addLast(y)
                        sunSources.addLast(z - 1)
                    }
                }
                // Y-propagation
                if (y < bounds) {
                    peek(x, y + 1, z, sideCell)
                    val llTop = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSide.BOTTOM)
                    if (!sideCell.voxel.opaque && sideCell.sunlight < llTop - 1) {
                        sideCell.sunlight = llTop - 1
                        poke(sideCell)
                        modifiedBlocks++
                        sunSources.addLast(x)
                        sunSources.addLast(y + 1)
                        sunSources.addLast(z)
                    }
                }
                if (y > -bounds) {
                    peek(x, y - 1, z, sideCell)
                    val llBottom = ll - sideCell.voxel.getLightLevelModifier(sideCell, cell, VoxelSide.TOP)
                    if (!sideCell.voxel.opaque && sideCell.sunlight < llBottom) {
                        sideCell.sunlight = llBottom
                        poke(sideCell)
                        modifiedBlocks++
                        sunSources.addLast(x)
                        sunSources.addLast(y - 1)
                        sunSources.addLast(z)
                    }
                }
            }
        }
        return modifiedBlocks
    }

    private inline fun peekRawFast(x: Int, y: Int, z: Int): Int =
            if (x in 0..31 && y in 0..31 && z in 0..31)
                chunk.peekRaw(x, y, z)
            else world.peekRaw(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32)

    private inline fun peek(x: Int, y: Int, z: Int, cell: ScratchCell) {
        cell.x = x
        cell.y = y
        cell.z = z
        val rawData = peekRawFast(x, y, z)
        cell.voxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(rawData)) ?: world.content.voxels().air()
        cell.sunlight = VoxelFormat.sunlight(rawData)
        cell.blocklight = VoxelFormat.blocklight(rawData)
        cell.metaData = VoxelFormat.meta(rawData)
    }

    private fun pokeRawFast(x: Int, y: Int, z: Int, data: Int) {
        // Still within bounds !
        if (x in 0..31 && y in 0..31 && z in 0..31) {
            chunk.pokeRawSilently(x, y, z, data)
        } else {

            val oldData = world.peekRaw(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32)
            world.pokeRawSilently(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32, data)

            val chunk = world.getChunkWorldCoordinates(x + chunkX * 32, y + chunkY * 32, z + chunkZ * 32)
            if (chunk != null && oldData != data)
                chunk.lightBaker.requestUpdate()

            return
        }
    }

    private fun poke(cell: ScratchCell) {
        val data = VoxelFormat.format(world.contentTranslator.getIdForVoxel(cell.voxel), cell.metaData, cell.sunlight, cell.blocklight)
        pokeRawFast(cell.x, cell.y, cell.z, data)
    }

    companion object {
        /* Ressources for actual computations */
        internal val sunlightMask = 0x000F0000
        internal val blocklightMask = 0x00F00000
        internal val sunAntiMask = -0xf0001
        internal val blockAntiMask = -0xf00001
        internal val sunBitshift = 0x10
        internal val blockBitshift = 0x14

        internal var tl_blockSources: ThreadLocal<IntDeque> = object : ThreadLocal<IntDeque>() {
            override fun initialValue(): IntDeque {
                return IntArrayDeque()
            }
        }
        internal var tl_sunSources: ThreadLocal<IntDeque> = object : ThreadLocal<IntDeque>() {
            override fun initialValue(): IntDeque {
                return IntArrayDeque()
            }
        }
        internal var tl_blockSourcesRemoval: ThreadLocal<IntDeque> = object : ThreadLocal<IntDeque>() {
            override fun initialValue(): IntDeque {
                return IntArrayDeque()
            }
        }
        internal var tl_sunSourcesRemoval: ThreadLocal<IntDeque> = object : ThreadLocal<IntDeque>() {
            override fun initialValue(): IntDeque {
                return IntArrayDeque()
            }
        }
    }
}