package xyz.chunkstories.graphics.common.world

import com.carrotsearch.hppc.IntArrayDeque
import org.joml.Vector2f
import org.joml.Vector2i
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import xyz.chunkstories.api.world.World
import xyz.chunkstories.graphics.common.Cleanable
import java.lang.Math.min

class FarTerrainCellHelper(val world: World) : Cleanable {
    val maxDistanceToRender = min(4096, world.worldInfo.size.sizeInChunks * 32)
    var currentSnappedCameraPos = Vector2i(-1000)

    lateinit var currentGrid: IntArrayDeque

    init {
        update(Vector2i(0, 0))
    }

    fun update(cameraPosition: Vector2i) {
        val snappedCameraPos = Vector2i((cameraPosition.x / 32) * 32, (cameraPosition.y / 32) * 32)

        if (snappedCameraPos != currentSnappedCameraPos) {
            currentSnappedCameraPos = snappedCameraPos

            currentGrid = drawGrid(5)
        }
    }

    val sizes = intArrayOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768)

    fun drawGrid(maxSubdivisions: Int): IntArrayDeque {
        val minLevel = 5

        val centerX = currentSnappedCameraPos.x
        val centerZ = currentSnappedCameraPos.y
        val bottomCornerX = centerX - maxDistanceToRender / 2
        val bottomCornerZ = centerZ - maxDistanceToRender / 2

        val cam = Vector2f(centerX + 16f, centerZ + 16f)

        var stack = IntArrayDeque()
        var out = IntArrayDeque()

        stack.addLast(bottomCornerX, bottomCornerZ, 12)

        val depth = 2
        for (d in 0..depth) {
            out.clear()
            while (!stack.isEmpty) {
                val size = stack.removeLast()
                val rsize = sizes[size]
                val oz = stack.removeLast()
                val ox = stack.removeLast()

                // should we subdivide this ?
                val smaller = size - 1
                val smallerSize = sizes[smaller]
                val cx = ox + smallerSize
                val cz = oz + smallerSize

                val d = Vector2f(cx + 0f, cz + 0f).distance(cam)
                val id = 1.0f / d

                val target = 5.0
                val subdivide = (rsize < target * id)

                if (subdivide) {
                    out.addLast(bottomCornerX + 0)
                    out.addLast(bottomCornerZ + 0)
                    out.addLast(smaller)

                    out.addLast(bottomCornerX + smallerSize)
                    out.addLast(bottomCornerZ + 0)
                    out.addLast(smaller)

                    out.addLast(bottomCornerX + 0)
                    out.addLast(bottomCornerZ + smallerSize)
                    out.addLast(smaller)

                    out.addLast(bottomCornerX + smallerSize)
                    out.addLast(bottomCornerZ + smallerSize)
                    out.addLast(smaller)
                }
            }

            // swap the two buffers
            val tmp = stack
            stack = out
            out = tmp
        }

        return stack
    }

    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}