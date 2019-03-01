package xyz.chunkstories.graphics.common

import org.joml.Vector3f

/** Reference cube object on hand */
object UnitCube {
    data class CubeFaceData(val normalDirection: Vector3f, val vertices: List<CubeVertexAttributes>)
    data class CubeVertexAttributes(val position: FloatArray, val textureCoordinates: FloatArray)

    /** X- */
    val leftFace = CubeFaceData(Vector3f(-1f, 0f, 0f), listOf(
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(1.0f, 1.0f))))

    /** Z+ */
    val frontFace = CubeFaceData(Vector3f(0f, 0f, 1f), listOf(
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(1.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 0.0f))))

    /** X+ */
    val rightFace = CubeFaceData(Vector3f(1f, 0f, 0f), listOf(
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 1.0f))))

    /** Z- */
    val backFace = CubeFaceData(Vector3f(0f, 0f, -1f), listOf(
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 0.0f))))

    /** Y+ */
    val topFace = CubeFaceData(Vector3f(0f, 1f, 0f), listOf(
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 0.0f))))

    /** Y- */
    val bottomFace = CubeFaceData(Vector3f(0f, -1f, 0f), listOf(
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            CubeVertexAttributes(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 1.0f)),
            CubeVertexAttributes(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(1.0f, 1.0f))))

    val faces = listOf(leftFace, frontFace, rightFace, backFace, topFace, bottomFace)
}