//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.animation

import org.joml.*

import xyz.chunkstories.api.animation.Animation

import xyz.chunkstories.api.math.Math2
import xyz.chunkstories.api.math.Quaternion4d
import java.lang.Math

class BiovisionBone(override val name: String, val channels: Int, private val animationDataOffset: Int, val offset: Vector3fc, val children: List<BiovisionBone>) : Animation.SkeletonBone {
    lateinit var animation: BiovisionAnimation
    override var parent: BiovisionBone? = null

    /**
     * Returns a Matrix4f describing how to end up at the bone transformation at the
     * given frame.
     *
     * @param frameLower
     * @return
     */
    fun getTransformationMatrixInterpolatedRecursive(frameLower: Int, frameUpper: Int, t: Double): Matrix4f {
        val matrix = getTransformationMatrixInterpolatedInternal(frameLower, frameUpper, t)

        // Apply the father transformation
        if (parent != null) {
            parent!!.getTransformationMatrixInterpolatedRecursive(frameLower, frameUpper, t).mul(matrix, matrix)
        }

        return matrix
    }

    override fun getTransformationMatrix(animationTime: Double): Matrix4f {
        val frame = animationTime / 1000.0 / animation.frameTime

        val frameUpperBound = Math.ceil(frame)
        val frameLowerBound = Math.floor(frame)

        var interp = frame % 1.0
        // Don't try to interpolate if we're on an exact frame
        if (frameLowerBound == frameUpperBound)
            interp = 0.0

        val frameLower = frameLowerBound.toInt() % animation.frames
        val frameUpper = frameUpperBound.toInt() % animation.frames

        return getTransformationMatrixInterpolatedInternal(frameLower, frameUpper, interp)
    }

    private fun getTransformationMatrixInterpolatedInternal(frameLower: Int, frameUpper: Int, t: Double): Matrix4f {
        val animationData = animation.animationData

        // Read rotation data from where it is
        val rotXLower: Float
        val rotYLower: Float
        val rotZLower: Float
        if (channels == 6) {
            rotXLower = toRad(animationData[frameLower][animationDataOffset + 3])
            rotYLower = toRad(animationData[frameLower][animationDataOffset + 4])
            rotZLower = toRad(animationData[frameLower][animationDataOffset + 5])
        } else {
            rotXLower = toRad(animationData[frameLower][animationDataOffset + 0])
            rotYLower = toRad(animationData[frameLower][animationDataOffset + 1])
            rotZLower = toRad(animationData[frameLower][animationDataOffset + 2])
        }

        val rotXUpper: Float
        val rotYUpper: Float
        val rotZUpper: Float
        if (channels == 6) {
            rotXUpper = toRad(animationData[frameUpper][animationDataOffset + 3])
            rotYUpper = toRad(animationData[frameUpper][animationDataOffset + 4])
            rotZUpper = toRad(animationData[frameUpper][animationDataOffset + 5])
        } else {
            rotXUpper = toRad(animationData[frameUpper][animationDataOffset + 0])
            rotYUpper = toRad(animationData[frameUpper][animationDataOffset + 1])
            rotZUpper = toRad(animationData[frameUpper][animationDataOffset + 2])
        }

        val quaternionXLower = Quaternion4d.fromAxisAngle(Vector3d(1.0, 0.0, 0.0), rotXLower.toDouble())
        val quaternionYLower = Quaternion4d.fromAxisAngle(Vector3d(0.0, 1.0, 0.0), rotYLower.toDouble())
        val quaternionZLower = Quaternion4d.fromAxisAngle(Vector3d(0.0, 0.0, 1.0), rotZLower.toDouble())
        val totalLower = quaternionXLower.mult(quaternionYLower).mult(quaternionZLower)

        val quaternionXUpper = Quaternion4d.fromAxisAngle(Vector3d(1.0, 0.0, 0.0), rotXUpper.toDouble())
        val quaternionYUpper = Quaternion4d.fromAxisAngle(Vector3d(0.0, 1.0, 0.0), rotYUpper.toDouble())
        val quaternionZUpper = Quaternion4d.fromAxisAngle(Vector3d(0.0, 0.0, 1.0), rotZUpper.toDouble())
        val totalUpper = quaternionXUpper.mult(quaternionYUpper).mult(quaternionZUpper)

        val total = Quaternion4d.slerp(totalLower, totalUpper, t)

        val matrix = total.toMatrix4f()

        // Apply transformations
        if (channels == 6) {
            matrix.m30(matrix.m30() + Math2.mix(animationData[frameLower][animationDataOffset + 0].toDouble(), animationData[frameUpper][animationDataOffset + 0].toDouble(), t))
            matrix.m31(matrix.m31() + Math2.mix(animationData[frameLower][animationDataOffset + 1].toDouble(), animationData[frameUpper][animationDataOffset + 1].toDouble(), t))
            matrix.m32(matrix.m32() + Math2.mix(animationData[frameLower][animationDataOffset + 2].toDouble(), animationData[frameUpper][animationDataOffset + 2].toDouble(), t))
        } else {
            matrix.m30(matrix.m30() + offset.x())
            matrix.m31(matrix.m31() + offset.y())
            matrix.m32(matrix.m32() + offset.z())
        }// TODO check on that, I'm not sure if you should apply both when possible

        return BiovisionAnimation.transformBlenderBVHExportToChunkStoriesWorldSpace(matrix)
    }

    private fun toRad(f: Float): Float {
        return (f / 180 * Math.PI).toFloat()
    }

    override fun toString(): String {
        var txt = ""
        var p = parent
        while (p != null) {
            txt += "\t"
            p = p.parent
        }
        txt += name + " " + channels + " channels, offset=" + offset!!.toString()// + ", dest=" + dest + "\n";
        for (c in children)
            txt += c.toString()

        return "[BVHTreeBone$txt]"
    }
}
