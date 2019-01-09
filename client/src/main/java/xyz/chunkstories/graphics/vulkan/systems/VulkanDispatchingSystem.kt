package xyz.chunkstories.graphics.vulkan.systems

import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.Cleanable

abstract class VulkanDispatchingSystem(val backend: VulkanGraphicsBackend) : DispatchingSystem, Cleanable {

}