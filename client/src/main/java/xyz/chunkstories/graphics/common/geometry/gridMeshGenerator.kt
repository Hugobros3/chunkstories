package xyz.chunkstories.graphics.common.geometry

import com.carrotsearch.hppc.IntArrayList
import org.joml.Vector3f
import org.joml.Vector3fc

fun generateGridVerticesAndIndices(size: Int): Pair<List<Vector3fc>, IntArray> {
    val indicesList = IntArrayList()
    val verticesList = mutableListOf<Vector3fc>()
    for(vz in 0..size) {
        for(vx in 0..size) {
            verticesList.add(Vector3f(vx.toFloat(), 0.0f, vz.toFloat()))
        }
    }

    fun vertIndex(x: Int, z: Int) = (z * (size + 1) + x)

    for(cz in 0 until size) {
        for(cx in 0 until size) {
            val vi0 = vertIndex(cx, cz)
            val vi1 = vertIndex(cx + 1, cz)
            val vi2 = vertIndex(cx, cz + 1)
            val vi3 = vertIndex(cx + 1, cz + 1)
            indicesList.add(vi0)
            indicesList.add(vi1)
            indicesList.add(vi3)

            indicesList.add(vi0)
            indicesList.add(vi3)
            indicesList.add(vi2)
        }
    }

    return Pair(verticesList, indicesList.toArray())
}