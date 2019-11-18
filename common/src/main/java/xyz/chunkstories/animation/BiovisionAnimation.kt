//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.animation

import org.joml.Matrix4f
import org.joml.Vector3f
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.animation.Animation

class BiovisionAnimation(val frames: Int, val frameTime: Float, val root: BiovisionBone, val bones: Map<String, BiovisionBone>, val animationData: Array<FloatArray>) : Animation {

    override fun getBoneHierarchyTransformationMatrix(nameOfEndBone: String, animationTime: Double): Matrix4f {
        var matrix = Matrix4f()
        if (frames == 0) {
            logger
            return matrix
        }

        val frame = animationTime / 1000.0 / frameTime.toDouble()

        val frameUpperBound = Math.ceil(frame)
        val frameLowerBound = Math.floor(frame)

        var interp = frame % 1.0
        // Don't try to interpolate if we're on an exact frame
        if (frameLowerBound == frameUpperBound)
            interp = 0.0

        val frameLower = frameLowerBound.toInt() % frames
        val frameUpper = frameUpperBound.toInt() % frames

        matrix = getBone(nameOfEndBone)!!.getTransformationMatrixInterpolatedRecursive(frameLower, frameUpper, interp)

        return matrix
    }

    override fun getOffsetMatrix(boneName: String): Matrix4f {
        val offsetMatrix = Matrix4f()
        val offsetTotal = Vector3f()

        val bone = getBone(boneName)

        // Accumulate the transformation offset
        var currentBone = bone
        while (currentBone != null) {
            // Coordinates systems n stuff
            offsetTotal.x = offsetTotal.x() + currentBone.offset.y()
            offsetTotal.y = offsetTotal.y() + currentBone.offset.z()
            offsetTotal.z = offsetTotal.z() + currentBone.offset.x()
            currentBone = currentBone.parent
        }

        // Negate it and build the offset matrix
        offsetTotal.negate()
        offsetMatrix.m30(offsetMatrix.m30() + offsetTotal.x())
        offsetMatrix.m31(offsetMatrix.m31() + offsetTotal.y())
        offsetMatrix.m32(offsetMatrix.m32() + offsetTotal.z())

        return offsetMatrix
    }

    override fun getBoneHierarchyTransformationMatrixWithOffset(boneName: String, animationTime: Double): Matrix4f {
        var matrix: Matrix4f? = null
        if (frames == 0) {
            println("Invalid bone : " + boneName + "in animation" + this)
            return Matrix4f()
        }

        // Get the normal bone transformation
        matrix = getBoneHierarchyTransformationMatrix(boneName, animationTime)

        // Apply the offset matrix
        matrix.mul(getOffsetMatrix(boneName))

        return matrix
    }

    override fun getBone(boneName: String): BiovisionBone? {
        val bone = bones[boneName]
        return bone ?: root
    }

    override fun toString(): String {
        var txt = "[BVH Animation File\n"
        txt += root.toString()
        txt += "Frames: $frames\n"
        txt += "Frame Time: $frameTime]"
        return txt
    }

    companion object {
        val logger = LoggerFactory.getLogger("animations")

        fun transformBlenderBVHExportToChunkStoriesWorldSpace(matrix: Matrix4f): Matrix4f {
            val blender2ingame = Matrix4f()
            // Cs & Blender conventions are both right-handed, ez way to map them is
            // Blender | Chunk Stories
            // +X      | +Z
            // +Y      | +X
            // +Z      | +Y
            blender2ingame.m00(0.0f)
            blender2ingame.m11(0.0f)
            blender2ingame.m22(0.0f)

            blender2ingame.m02(1.0f)
            blender2ingame.m10(1.0f)
            blender2ingame.m21(1.0f)

            // Rotate the matrix first to apply the transformation in blender space
            blender2ingame.mul(matrix, matrix)

            //Matrix4f out = new Matrix4f(blender2ingame);
            //out.invert();
            val out = Matrix4f()
            out.m00(0.0f)
            out.m11(0.0f)
            out.m22(0.0f)

            out.m20(1.0f)
            out.m01(1.0f)
            out.m12(1.0f)

            matrix.mul(out, matrix)

            return matrix
        }
    }
}
