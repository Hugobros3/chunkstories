package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import kotlin.contracts.*

//TODO test if inline helps (or if HotSpot does it by itself)
public fun Int.ensureIs(exceptionMessage: String, compareTo: Int) = if (this != compareTo) throw Exception(exceptionMessage) else Unit

public fun Int.ensureIs(exceptionMessage: String, vararg compareTo: Int) = if (!compareTo.contains(this)) throw Exception(exceptionMessage) else Unit

operator fun PointerBuffer.iterator(): Iterator<Long> = object : Iterator<Long> {
    var index = 0

    override fun next(): Long = this@iterator.get(index++)
    override fun hasNext(): Boolean = index < this@iterator.limit()
}

/*
@ExperimentalContracts
inline fun <T : Any?> stack(operations: () -> T) : T {
    kotlin.contracts.contract {
        callsInPlace(operations, InvocationKind.EXACTLY_ONCE)
    }

    MemoryStack.stackPush()
    val t = operations()
    MemoryStack.stackPop()

    return t
}*/