package xyz.chunkstories.graphics.vulkan.resources.frameallocator

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame

/** Provides and recycles the FrameDataAllocator objects */
interface FrameDataAllocatorProvider: Cleanable {
    fun beginFrame(frame: VulkanFrame): FrameDataAllocator

    fun retireFrame(frame: VulkanFrame)
}

fun VulkanGraphicsBackend.createFrameDataAllocatorProvider(): FrameDataAllocatorProvider {
    return NaiveFrameDataAllocatorProvider(this)
}

abstract class GenericPerFrameDataProvider : FrameDataAllocatorProvider

/** Uses that sweet 256MiB direct window to GPU memory on AMD hardware */
abstract class AMD_Gcn_PerFrameDataProvider : FrameDataAllocatorProvider