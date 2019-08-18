//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.iterators

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.util.IterableIterator
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelFormat
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.cell.Cell

private val Box.xWidth: Double
    get() = extents.x()
private val Box.yHeight: Double
    get() = extents.y()
private val Box.zWidth: Double
    get() = extents.z()

private val Box.xPosition: Double
    get() = min.x()
private val Box.yPosition: Double
    get() = min.y()
private val Box.zPosition: Double
    get() = min.z()

class BlocksInBoundingBoxIterator(override val world: World, val box: Box) : IterableIterator<Cell>, Cell {

    // private final Voxels voxels;

    private var i: Int = 0
    private var j: Int = 0
    private var k: Int = 0
    override var x: Int = 0
        private set
    override var y: Int = 0
        private set
    override var z: Int = 0
        private set

    private val minx: Int
    private val miny: Int
    private val minz: Int
    private val maxx: Int
    private val maxy: Int
    private val maxz: Int

    override lateinit var voxel: Voxel
        private set
    override var sunlight: Int = 0
        private set
    override var blocklight: Int = 0
        private set
    override var metaData: Int = 0
        private set

    override val location: Location
        get() = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

    override val translatedCollisionBoxes: Array<Box>?
        get() = voxel.getTranslatedCollisionBoxes(this)

    init {

        // this.voxels = world.getGameContext().getContent().voxels();

        this.minx = Math.floor(box.xPosition).toInt()
        this.miny = Math.floor(box.yPosition).toInt()
        this.minz = Math.floor(box.zPosition).toInt()

        this.maxx = Math.ceil(box.xPosition + box.xWidth).toInt()
        this.maxy = Math.ceil(box.yPosition + box.yHeight).toInt()
        this.maxz = Math.ceil(box.zPosition + box.zWidth).toInt()

        this.i = minx
        this.j = miny
        this.k = minz
    }

    override fun hasNext(): Boolean {
        return k <= maxz
        /*
		 * if(i == maxx && j == maxy && k == maxz) return false; return true;
		 */
        // return k <= (int)Math.ceil(box.zpos + box.zw);
    }

    override fun next(): Cell {

        x = i
        y = j
        z = k

        i++
        if (i > maxx) {
            j++
            i = minx
        }
        if (j > maxy) {
            k++
            j = miny
        }
        if (k > maxz) {

        } // throw new UnsupportedOperationException("Out of bounds iterator. Called when
        // hasNext() returned false.");

        // Optimisation here:
        // Instead of making a new CellData object for each iteration we just change
        // this one by pulling the properties
        val raw_data = world.peekRaw(x, y, z)
        voxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(raw_data))!!
        sunlight = VoxelFormat.sunlight(raw_data)
        blocklight = VoxelFormat.blocklight(raw_data)
        metaData = VoxelFormat.meta(raw_data)

        return this
    }

    override fun getNeightbor(side_int: Int): Cell {
        val side = VoxelSide.values()[side_int]
        return world.peek(x + side.dx, y + side.dy, z + side.dz)
    }

    override fun getNeightborMetadata(i: Int): Int {
        return 0
    }

    override fun getNeightborVoxel(i: Int): Voxel? {
        return null
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }
}
