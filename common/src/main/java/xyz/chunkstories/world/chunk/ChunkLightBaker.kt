//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.chunk

import java.util.concurrent.locks.ReentrantLock

import com.carrotsearch.hppc.IntArrayDeque
import com.carrotsearch.hppc.IntDeque
import xyz.chunkstories.EngineImplemI

import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.chunk.ChunkLightUpdater
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

//TODO use custom propagation for ALL propagation functions & cleanup this whole darn mess
/** Responsible for propagating voxel volumetric light  */
class ChunkLightBaker(internal val chunk: ChunkImplementation) : AutoRebuildingProperty((chunk.world.gameInstance as EngineImplemI).tasks, true), ChunkLightUpdater {
    internal val world = chunk.world
    internal val chunkX: Int = chunk.chunkX
    internal val chunkY: Int = chunk.chunkY
    internal val chunkZ: Int = chunk.chunkZ

    internal val lightDataLock = ReentrantLock()

    override fun createTask(updatesToConsider: Int): UpdateTask = TaskLightChunk(this, updatesToConsider, true)

    /** Hack until we move to fibers/coroutines: immediately do the lighting baking ( to avoid worker gridlocking ) */
    fun hackyUpdateDirect() {
        val task = TaskLightChunk(this, 0, true)
        // TODO task.findAndSpreadLight()
    }

    /** Called when a voxel is changed  */
    fun computeLightSpread(bx: Int, by: Int, bz: Int, dataBefore: Int, data: Int) {
        /*try {
            lightDataLock.lock()

            val sunLightBefore = VoxelFormat.sunlight(dataBefore)
            val blockLightBefore = VoxelFormat.blocklight(dataBefore)

            var sunLightAfter = VoxelFormat.sunlight(data)
            val blockLightAfter = VoxelFormat.blocklight(data)

            val csh = world.heightmapsManager.getHeight(bx + chunkX * 32, bz + chunkZ * 32)
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
        }*/
    }

    inner class TaskLightChunk(baker: ChunkLightBaker, private val updatesToCommit: Int, private val considerAdjacentChunks: Boolean) : AutoRebuildingProperty.UpdateTask(baker, updatesToCommit) {
        private val leftChunk: ChunkImplementation?
        private val rightChunk: ChunkImplementation?
        private val topChunk: ChunkImplementation?
        private val bottomChunk: ChunkImplementation?
        private val frontChunk: ChunkImplementation?
        private val backChunk: ChunkImplementation?

        init {
            // Checks if the adjacent chunks are done loading
            topChunk = world.chunksManager.getChunk(chunkX, chunkY + 1, chunkZ)
            bottomChunk = world.chunksManager.getChunk(chunkX, chunkY - 1, chunkZ)
            frontChunk = world.chunksManager.getChunk(chunkX, chunkY, chunkZ + 1)
            backChunk = world.chunksManager.getChunk(chunkX, chunkY, chunkZ - 1)
            leftChunk = world.chunksManager.getChunk(chunkX - 1, chunkY, chunkZ)
            rightChunk = world.chunksManager.getChunk(chunkX + 1, chunkY, chunkZ)
        }

        override fun update(taskExecutor: TaskExecutor): Boolean {
            if (updatesToCommit == 0)
                return true

            // Actual computation takes place here
            // TODO findAndSpreadLight()

            return true
        }

        /*internal fun findAndSpreadLight(): Int {
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
        }*/

        /*private fun propagateLightSources(blockSources: IntDeque, sunSources: IntDeque, propagateToAdjacentChunks: Boolean): Int {
            var modifiedBlocks = 0

            // Don't spam the requeue requests
            var checkTopBleeding = propagateToAdjacentChunks    // && topChunk != null
            var checkBottomBleeding = propagateToAdjacentChunks // && bottomChunk != null
            var checkFrontBleeding = propagateToAdjacentChunks  // && frontChunk != null
            var checkBackBleeding = propagateToAdjacentChunks   // && backChunk != null
            var checkLeftBleeding = propagateToAdjacentChunks   // && leftChunk != null
            var checkRightBleeding = propagateToAdjacentChunks  // && rightChunk != null

            if (propagateToAdjacentChunks) {
                assert(topChunk != null)
                assert(bottomChunk != null)
                assert(frontChunk != null)
                assert(backChunk != null)
                assert(leftChunk != null)
                assert(rightChunk != null)
            }

            var requestTop = false
            var requestBot = false
            var requestFront = false
            var requestBack = false
            var requestLeft = false
            var requestRight = false

            while (blockSources.size() > 0) {
                val z = blockSources.removeLast()
                val y = blockSources.removeLast()
                val x = blockSources.removeLast()

                val cellData = getCellDataMutFast(x, y, z)
                var cellLightLevel = cellData.blocklightLevel

                if (cellData.blockType.opaque)
                    cellLightLevel = cellData.blockType.getEmittedLightLevel(cellData)

                if (cellLightLevel > 1) {
                    if (x < 31) {
                        val adj = getCellDataMutFast(x + 1, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.LEFT) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            adj.blocklightLevel = fadedLightLevel
                            modifiedBlocks++
                            blockSources.addLast(x + 1)
                            blockSources.addLast(y)
                            blockSources.addLast(z)
                        }
                    } else if (checkRightBleeding) {
                        val adj = getCellDataMutFast(32, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.LEFT) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            requestRight = true
                            checkRightBleeding = false
                        }
                    }
                    if (x > 0) {
                        val adj = getCellDataMutFast(x - 1, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.RIGHT) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            adj.blocklightLevel = fadedLightLevel
                            modifiedBlocks++
                            blockSources.addLast(x - 1)
                            blockSources.addLast(y)
                            blockSources.addLast(z)
                        }
                    } else if (checkLeftBleeding) {
                        val adj = getCellDataMutFast(-1, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.RIGHT) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            requestLeft = true
                            checkLeftBleeding = false
                        }
                    }

                    if (z < 31) {
                        val adj = getCellDataMutFast(x, y, z + 1)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.BACK) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            adj.blocklightLevel = fadedLightLevel
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y)
                            blockSources.addLast(z + 1)
                        }
                    } else if (checkFrontBleeding) {
                        val adj = getCellDataMutFast(x, y, 32)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.BACK) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            requestFront = true
                            checkFrontBleeding = false
                        }
                    }
                    if (z > 0) {
                        val adj = getCellDataMutFast(x, y, z - 1)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.FRONT) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            adj.blocklightLevel = fadedLightLevel
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y)
                            blockSources.addLast(z - 1)
                        }
                    } else if (checkBackBleeding) {
                        val adj = getCellDataMutFast(x, y, -1)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.FRONT) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            requestBack = true
                            checkBackBleeding = false
                        }
                    }

                    if (y < 31) {
                        val adj = getCellDataMutFast(x, y + 1, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.BOTTOM) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            adj.blocklightLevel = fadedLightLevel
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y + 1)
                            blockSources.addLast(z)
                        }
                    } else if (checkTopBleeding) {
                        val adj = getCellDataMutFast(x, 32, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.BOTTOM) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            requestTop = true
                            checkTopBleeding = false
                        }
                    }

                    if (y > 0) {
                        val adj = getCellDataMutFast(x, y - 1, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.TOP) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
                            adj.blocklightLevel = fadedLightLevel
                            modifiedBlocks++
                            blockSources.addLast(x)
                            blockSources.addLast(y - 1)
                            blockSources.addLast(z)
                        }
                    } else if (checkBottomBleeding) {
                        val adj = getCellDataMutFast(x, -1, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cellData, BlockSide.TOP) + 1)
                        if (adj.blocklightLevel < fadedLightLevel) {
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

                val cell = getCellDataMutFast(x, y, z)
                var cellLightLevel = cell.sunlightLevel

                if (cell.blockType.opaque)
                    cellLightLevel = cell.blockType.getEmittedLightLevel(cell)

                if (cellLightLevel > 1) {
                    if (x < 31) {
                        val adj = getCellDataMutFast(x + 1, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.LEFT) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            adj.sunlightLevel = fadedLightLevel
                            modifiedBlocks++
                            sunSources.addLast(x + 1)
                            sunSources.addLast(y)
                            sunSources.addLast(z)
                        }
                    } else if (checkRightBleeding) {
                        val adj = getCellDataMutFast(32, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.LEFT) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            requestRight = true
                            checkRightBleeding = false
                        }
                    }
                    if (x > 0) {
                        val adj = getCellDataMutFast(x - 1, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.RIGHT) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            adj.sunlightLevel = fadedLightLevel
                            modifiedBlocks++
                            sunSources.addLast(x - 1)
                            sunSources.addLast(y)
                            sunSources.addLast(z)
                        }
                    } else if (checkLeftBleeding) {
                        val adj = getCellDataMutFast(-1, y, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.RIGHT) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            requestLeft = true
                            checkLeftBleeding = false
                        }
                    }

                    if (z < 31) {
                        val adj = getCellDataMutFast(x, y, z + 1)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.BACK) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            adj.sunlightLevel = fadedLightLevel
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y)
                            sunSources.addLast(z + 1)
                        }
                    } else if (checkFrontBleeding) {
                        val adj = getCellDataMutFast(x, y, 32)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.BACK) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            requestFront = true
                            checkFrontBleeding = false
                        }
                    }
                    if (z > 0) {
                        val adj = getCellDataMutFast(x, y, z - 1)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.FRONT) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            adj.sunlightLevel = fadedLightLevel
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y)
                            sunSources.addLast(z - 1)
                        }
                    } else if (checkBackBleeding) {
                        val adj = getCellDataMutFast(x, y, -1)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.FRONT) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            requestBack = true
                            checkBackBleeding = false
                        }
                    }

                    if (y < 31) {
                        val adj = getCellDataMutFast(x, y + 1, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.BOTTOM) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            adj.sunlightLevel = fadedLightLevel
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y + 1)
                            sunSources.addLast(z)
                        }
                    } else if (checkTopBleeding) {
                        val adj = getCellDataMutFast(x, 32, z)
                        val fadedLightLevel = cellLightLevel - (adj.blockType.getLightLevelModifier(adj, cell, BlockSide.BOTTOM) + 1)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            requestTop = true
                            checkTopBleeding = false
                        }
                    }

                    // Special case! This is the bottom computation for light spread, light doesn't
                    // fade when traveling backwards so we do not decrement fadedLightLevel !
                    if (y > 0) {
                        val adj = getCellDataMutFast(x, y - 1, z)
                        val fadedLightLevel = cellLightLevel - adj.blockType.getLightLevelModifier(adj, cell, BlockSide.TOP)
                        if (adj.sunlightLevel < fadedLightLevel) {
                            adj.sunlightLevel = fadedLightLevel
                            modifiedBlocks++
                            sunSources.addLast(x)
                            sunSources.addLast(y - 1)
                            sunSources.addLast(z)
                        }
                    } else if (checkBottomBleeding) {
                        val adj = getCellDataMutFast(x, -1, z)
                        val fadedLightLevel = cellLightLevel - adj.blockType.getLightLevelModifier(adj, cell, BlockSide.TOP)
                        if (adj.sunlightLevel < fadedLightLevel) {
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
        }*/

        /*private fun findLightSources(blockSources: IntDeque, sunSources: IntDeque) {
            for (x in 0..31)
                for (z in 0..31) {
                    var y = 31
                    var hitGroundYet = false

                    val groundHeight = world.heightmapsManager.getHeight(chunkX * 32 + x, chunkZ * 32 + z)
                    while (y >= 0) {
                        val cell = getCellDataMutFast(x, y, z)
                        val emittedLight = cell.blockType.getEmittedLightLevel(cell)

                        if (emittedLight > 0) {
                            cell.blocklightLevel = emittedLight
                            blockSources.addLast(x)
                            blockSources.addLast(y)
                            blockSources.addLast(z)
                        }

                        if (!hitGroundYet && groundHeight != -1) {
                            if (chunkY * 32 + y >= groundHeight) {
                                if (chunkY * 32 + y <= groundHeight ||
                                        !world.contentTranslator.getVoxelForId(VoxelFormat.id(chunk.voxelDataArray!![x * 1024 + y * 32 + z]))!!.isAir)
                                    hitGroundYet = true
                                else {
                                    cell.sunlightLevel = 15
                                    sunSources.addLast(x)
                                    sunSources.addLast(y)
                                    sunSources.addLast(z)
                                }
                            }
                        }

                        y--
                    }
                }
        }*/

        /*private fun propagateLightFromBorders(blockSources: IntDeque, sunSources: IntDeque): Int {
            var mods = 0

            if (rightChunk != null) {
                for (z in 0..31)
                    for (y in 0..31) {
                        val adj = getCellDataMutFast(32, y, z)
                        val cell = getCellDataMutFast(31, y, z)

                        val modifier = cell.blockType.getLightLevelModifier(cell, adj, BlockSide.RIGHT) + 1
                        if (adj.blocklightLevel - modifier > cell.blocklightLevel) {
                            cell.blocklightLevel = adj.blocklightLevel - modifier
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlightLevel - modifier > cell.sunlightLevel) {
                            cell.sunlightLevel = adj.sunlightLevel - modifier
                            mods++
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }
            if (leftChunk != null) {
                for (z in 0..31)
                    for (y in 0..31) {
                        val adj = getCellDataMutFast(-1, y, z)
                        val cell = getCellDataMutFast(0, y, z)

                        val modifier = cell.blockType.getLightLevelModifier(cell, adj, BlockSide.LEFT) + 1
                        if (adj.blocklightLevel - modifier > cell.blocklightLevel) {
                            cell.blocklightLevel = adj.blocklightLevel - modifier
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlightLevel - modifier > cell.sunlightLevel) {
                            cell.sunlightLevel = adj.sunlightLevel - modifier
                            mods++
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }
            if (topChunk != null) {
                for (z in 0..31)
                    for (x in 0..31) {
                        val adj = getCellDataMutFast(x, 32, z)
                        val cell = getCellDataMutFast(x, 31, z)

                        var modifier = cell.blockType.getLightLevelModifier(cell, adj, BlockSide.TOP) + 1
                        if (adj.blocklightLevel - modifier > cell.blocklightLevel) {
                            cell.blocklightLevel = adj.blocklightLevel - modifier
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        modifier -= 1 // sunlight doesn't dim travelling downwards
                        if (adj.sunlightLevel - modifier > cell.sunlightLevel) {
                            cell.sunlightLevel = adj.sunlightLevel - modifier
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
                        val adj = getCellDataMutFast(x, -1, z)
                        val cell = getCellDataMutFast(x, 0, z)

                        val modifier = cell.blockType.getLightLevelModifier(cell, adj, BlockSide.BOTTOM) + 1
                        if (adj.blocklightLevel - modifier > cell.blocklightLevel) {
                            cell.blocklightLevel = adj.blocklightLevel - modifier
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlightLevel - modifier > cell.sunlightLevel) {
                            cell.sunlightLevel = adj.sunlightLevel - modifier
                            mods++
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
                        val adj = getCellDataMutFast(x, y, 32)
                        val cell = getCellDataMutFast(x, y, 31)

                        val modifier = cell.blockType.getLightLevelModifier(cell, adj, BlockSide.FRONT) + 1
                        if (adj.blocklightLevel - modifier > cell.blocklightLevel) {
                            cell.blocklightLevel = adj.blocklightLevel - modifier
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlightLevel - modifier > cell.sunlightLevel) {
                            cell.sunlightLevel = adj.sunlightLevel - modifier
                            mods++
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
                        val adj = getCellDataMutFast(x, y, -1)
                        val cell = getCellDataMutFast(x, y, 0)

                        val modifier = cell.blockType.getLightLevelModifier(cell, adj, BlockSide.BACK) + 1
                        if (adj.blocklightLevel - modifier > cell.blocklightLevel) {
                            cell.blocklightLevel = adj.blocklightLevel - modifier
                            mods++
                            blockSources.addLast(cell.x and 0x1f)
                            blockSources.addLast(cell.y and 0x1f)
                            blockSources.addLast(cell.z and 0x1f)
                        }
                        if (adj.sunlightLevel - modifier > cell.sunlightLevel) {
                            cell.sunlightLevel = adj.sunlightLevel - modifier
                            mods++
                            sunSources.addLast(cell.x and 0x1f)
                            sunSources.addLast(cell.y and 0x1f)
                            sunSources.addLast(cell.z and 0x1f)
                        }
                    }
            }

            return mods
        }*/
    }

    private fun getCellDataMutFast(lx: Int, ly: Int, lz: Int) =
            if (lx in 0..31 && ly in 0..31 && lz in 0..31)
                chunk.getCellMut(lx, ly, lz).data
            else world.getCellMut(lx + chunkX * 32, ly + chunkY * 32, lz + chunkZ * 32)!!.data

    override fun toString(): String {
        return "ChunkLightBaker(pending updates=$pendingUpdates)"
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