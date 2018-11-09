package io.xol.chunkstories.util.math

import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector3i

fun Vector3dc.toVec3f() = Vector3f(x().toFloat(), y().toFloat(), z().toFloat())

fun Vector3dc.toVec3i() = Vector3i(x().toInt(), y().toInt(), z().toInt())