package xyz.chunkstories.world.chunk

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelFormat
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.voxel.components.VoxelComponent
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.FreshChunkCell
import xyz.chunkstories.voxel.components.CellComponentsHolder

class ActualChunkVoxelContext(private val cubicChunk: CubicChunk, x: Int, y: Int, z: Int, data: Int) :
        Cell(x and 0x1F, y and 0x1F, z and 0x1F, cubicChunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(data))
                ?: cubicChunk.world.content.voxels().air()
                , VoxelFormat.meta(data), VoxelFormat.blocklight(data), VoxelFormat.sunlight(data)), ChunkCell, FreshChunkCell {
    override var data: Int = 0
        internal set

    override val world: World
        get() = cubicChunk.world

    override val x: Int
        get() = super.x + (cubicChunk.chunkX shl 5)

    override val y: Int
        get() = super.y + (cubicChunk.chunkY shl 5)

    override val z: Int
        get() = super.z + (cubicChunk.chunkZ shl 5)

    override val location: Location
        get() = Location(cubicChunk.world, x.toDouble(), y.toDouble(), z.toDouble())

    override val chunk: Chunk
        get() = cubicChunk

    override val components: CellComponentsHolder
        get() = cubicChunk.getComponentsAt(x, y, z)

    override var voxel: Voxel
        get() = super.voxel
        set(voxel) {
            super.voxel = voxel
            poke()
            peek()
        }

    override var metaData: Int
        get() = super.metaData
        set(metadata) {
            super.metaData = metadata
            poke()
            peek()
        }

    override var sunlight: Int
        get() = super.sunlight
        set(sunlight) {
            super.sunlight = sunlight
            poke()
            peek()
        }

    override var blocklight: Int
        get() = super.blocklight
        set(blocklight) {
            super.blocklight = blocklight
            poke()
            peek()
        }

    init {

        this.data = data
    }

    override fun refreshRepresentation() {
        //TODO
    }

    @Deprecated("")
    fun getNeightborData(side: Int): Int {
        when (side) {
            0 -> return cubicChunk.world.peekRaw(x - 1, y, z)
            1 -> return cubicChunk.world.peekRaw(x, y, z + 1)
            2 -> return cubicChunk.world.peekRaw(x + 1, y, z)
            3 -> return cubicChunk.world.peekRaw(x, y, z - 1)
            4 -> return cubicChunk.world.peekRaw(x, y + 1, z)
            5 -> return cubicChunk.world.peekRaw(x, y - 1, z)
        }
        throw RuntimeException("Pick a valid side")
    }

    override fun getNeightbor(side_int: Int): CellData {
        val side = VoxelSide.values()[side_int]

        // Fast path for in-chunk neigtbor
        return if (side == VoxelSide.LEFT && x > 0 || side == VoxelSide.RIGHT && x < 31
                || side == VoxelSide.BOTTOM && y > 0 || side == VoxelSide.TOP && y < 31
                || side == VoxelSide.BACK && z > 0 || side == VoxelSide.FRONT && z < 31) {
            cubicChunk.peek(x + side.dx, y + side.dy, z + side.dz)
        } else cubicChunk.world.peekSafely(x + side.dx, y + side.dy, z + side.dz)

    }

    private fun peek() {
        data = cubicChunk.peekRaw(x, y, z)
    }

    private fun poke() {
        cubicChunk.pokeSimple(x, y, z, voxel, sunlight, blocklight, metaData)
    }

    override fun registerComponent(name: String, component: VoxelComponent) {
        components.put(name, component)
    }
}
