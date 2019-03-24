package xyz.chunkstories.graphics.vulkan.util

import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.lwjgl.PointerBuffer
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import java.io.InputStream
import java.nio.ByteBuffer


import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLInstancedInput
import xyz.chunkstories.graphics.vulkan.buffers.extractInterfaceBlockField

//TODO test if inline helps (or if HotSpot does it by itself)
public fun Int.ensureIs(exceptionMessage: String, compareTo: Int) = if (this != compareTo) throw Exception("Unexpected return code: $this : $exceptionMessage") else Unit

public fun Int.ensureIs(exceptionMessage: String, vararg compareTo: Int) = if (!compareTo.contains(this)) throw Exception("Unexpected return code: $this : $exceptionMessage") else Unit

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
    val semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
    val pSemaphore = stackMallocLong(1)
    vkCreateSemaphore(this.logicalDevice.vkDevice, semaphoreCreateInfo, null, pSemaphore).ensureIs("Failed to create semaphore", VK_SUCCESS)
    val semaphore = pSemaphore.get(0)
    stackPop()

    return semaphore
}

fun VulkanGraphicsBackend.createFence(createSignalled : Boolean) : VkFence {
    stackPush()
    val fenceCreateInfo = VkFenceCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).apply {
        if(createSignalled) flags(VK_FENCE_CREATE_SIGNALED_BIT)
    }
    val pFence = stackMallocLong(1)
    vkCreateFence(this.logicalDevice.vkDevice, fenceCreateInfo, null, pFence).ensureIs("Failed to create semaphore", VK_SUCCESS)
    val fence = pFence.get(0)
    stackPop()

    return fence
}

fun VulkanGraphicsBackend.waitFence(fence: VkFence) {
    loop@ while(true) {
        val rslt = vkWaitForFences(this.logicalDevice.vkDevice, fence, true, 1000)
        when (rslt) {
            VK_SUCCESS -> break@loop
            VK_TIMEOUT -> continue@loop
            VK_ERROR_DEVICE_LOST -> throw Exception("well fuck")
        }
    }
}

fun getAlignedsizeForStruct(instancedStruct: GLSLInstancedInput): Int {
    //val instancedStruct = glslProgram.instancedInputs.find { it.name == name } ?: throw Exception("No instanced input named: $name")
    val structSize = instancedStruct.struct.size
    val sizeAligned16 = if (structSize % 16 == 0) structSize else (structSize / 16 * 16) + 16
    return sizeAligned16
}

fun writeInterfaceBlock(byteBuffer: ByteBuffer, offset: Int, interfaceBlock: InterfaceBlock, glslResource: GLSLInstancedInput) {
    byteBuffer.position(offset)

    for (field in glslResource.struct.fields) {
        byteBuffer.position(offset + field.offset)
        extractInterfaceBlockField(field, byteBuffer, interfaceBlock)
    }
}