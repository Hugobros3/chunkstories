package xyz.chunkstories.graphics.vulkan.util


import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFenceCreateInfo
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import java.io.InputStream
import java.nio.ByteBuffer

//TODO test if inline helps (or if HotSpot does it by itself)
public fun Int.ensureIs(exceptionMessage: String, compareTo: Int) = if (this != compareTo) throw Exception("Unexpected return code: $this : $exceptionMessage") else Unit

public fun Int.ensureIs(exceptionMessage: String, vararg compareTo: Int) = if (!compareTo.contains(this)) throw Exception("Unexpected return code: $this : $exceptionMessage") else Unit

operator fun PointerBuffer.iterator(): Iterator<Long> = object : Iterator<Long> {
    var index = 0

    override fun next(): Long = this@iterator.get(index++)
    override fun hasNext(): Boolean = index < this@iterator.limit()
}

fun InputStream.toByteBuffer(): ByteBuffer {
    this.use {
        val bytes = this.readBytes()
        val byteBuffer = ByteBuffer.allocateDirect(bytes.size)
        byteBuffer.put(bytes)
        byteBuffer.flip()
        return byteBuffer
    }
}

fun VulkanGraphicsBackend.createSemaphore(): VkSemaphore {
    stackPush()
    val semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
    val pSemaphore = stackMallocLong(1)
    vkCreateSemaphore(this.logicalDevice.vkDevice, semaphoreCreateInfo, null, pSemaphore).ensureIs("Failed to create semaphore", VK_SUCCESS)
    val semaphore = pSemaphore.get(0)
    stackPop()

    return semaphore
}

fun VulkanGraphicsBackend.createFence(createSignalled: Boolean): VkFence {
    stackPush()
    val fenceCreateInfo = VkFenceCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).apply {
        if (createSignalled) flags(VK_FENCE_CREATE_SIGNALED_BIT)
    }
    val pFence = stackMallocLong(1)
    vkCreateFence(this.logicalDevice.vkDevice, fenceCreateInfo, null, pFence).ensureIs("Failed to create semaphore", VK_SUCCESS)
    val fence = pFence.get(0)
    stackPop()

    return fence
}

fun VulkanGraphicsBackend.waitFence(fence: VkFence) {
    loop@ while (true) {
        val rslt = vkWaitForFences(this.logicalDevice.vkDevice, fence, true, 1000)
        when (rslt) {
            VK_SUCCESS -> break@loop
            VK_TIMEOUT -> continue@loop
            VK_ERROR_DEVICE_LOST -> throw Exception("well fuck")
        }
    }
}