//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.cell

import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.cell.Cell

/** Used to recycle results of a peek command  */
class ScratchCell(override val world: World) : Cell {
    // Fields set to public so we can access them
    override var x: Int = 0
    override var y: Int = 0
    override var z: Int = 0
    override lateinit var voxel: Voxel
    override var sunlight: Int = 0
    override var blocklight: Int = 0
    override var metaData: Int = 0

    override fun getNeightbor(side: Int): Cell {
        val side = VoxelSide.values()[side]
        return world.peek(x + side.dx, y + side.dy, z + side.dz)
    }
}
