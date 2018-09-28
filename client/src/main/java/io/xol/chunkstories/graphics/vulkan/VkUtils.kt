package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.contracts.*


import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

//TODO test if inline helps (or if HotSpot does it by itself)
public fun Int.ensureIs(exceptionMessage: String, compareTo: Int) = if (this != compareTo) throw Exception(exceptionMessage) else Unit

public fun Int.ensureIs(exceptionMessage: String, vararg compareTo: Int) = if (!compareTo.contains(this)) throw Exception(exceptionMessage) else Unit

operator fun PointerBuffer.iterator(): Iterator<Long> = object : Iterator<Long> {
    var index = 0

    override fun next(): Long = this@iterator.get(index++)
    override fun hasNext(): Boolean = index < this@iterator.limit()
}

fun InputStream.toByteBuffer() : ByteBuffer {
    this.use {
        val bytes = this.readBytes()
        val byteBuffer = ByteBuffer.allocateDirect(bytes.size)
        byteBuffer.put(bytes)
        byteBuffer.flip()
        return byteBuffer
    }
}

fun VulkanGraphicsBackend.createSemaphore() : VkSemaphore {
    stackPush()
    val semaphoreInfo = VkSemaphoreCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
    val pSemaphore = stackMallocLong(1)
    vkCreateSemaphore(this.logicalDevice.vkDevice, semaphoreInfo, null, pSemaphore).ensureIs("Failed to create semaphore", VK_SUCCESS)
    val semaphore = pSemaphore.get(0)
    stackPop()

    return semaphore
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