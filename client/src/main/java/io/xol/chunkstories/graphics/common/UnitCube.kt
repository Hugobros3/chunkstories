package io.xol.chunkstories.graphics.common

/** Reference cube object on hand */
object UnitCube {
    /** X- */
    val leftFace = listOf(Pair(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(1.0f, 0.0f)))

    /** Z+ */
    val frontFace = listOf(Pair(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(1.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 1.0f)))

    /** X+ */
    val rightFace = listOf(Pair(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 1.0f)),
            Pair(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 1.0f)),
            Pair(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 0.0f)))

    /** Z- */
    val backFace = listOf( Pair(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            Pair(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            Pair(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 1.0f)))

    /** Y+ */
    val topFace = listOf(Pair(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 0.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 1.0f, 0.0f), floatArrayOf(0.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 1.0f, 1.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 0.0f)))

    /** Y- */
    val bottomFace = listOf(Pair(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 0.0f, 0.0f), floatArrayOf(1.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 0.0f, 0.0f), floatArrayOf(0.0f, 0.0f)),
            Pair(floatArrayOf(1.0f, 0.0f, 1.0f), floatArrayOf(1.0f, 1.0f)),
            Pair(floatArrayOf(0.0f, 0.0f, 1.0f), floatArrayOf(0.0f, 1.0f)))

    val faces = listOf(leftFace, frontFace, rightFace, backFace, topFace, bottomFace)
}