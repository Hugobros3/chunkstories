package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable

abstract class VulkanDispatchingSystem(val backend: VulkanGraphicsBackend) : DispatchingSystem, Cleanable {

}