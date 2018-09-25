package io.xol.chunkstories.graphics.vulkan

//TODO test if inline helps (or if HotSpot does it by itself)
public fun Int.ensureIs(exceptionMessage: String, compareTo: Int) = if (this != compareTo) throw Exception(exceptionMessage) else Unit

public fun Int.ensureIs(exceptionMessage: String, vararg compareTo: Int) = if (!compareTo.contains(this)) throw Exception(exceptionMessage) else Unit

