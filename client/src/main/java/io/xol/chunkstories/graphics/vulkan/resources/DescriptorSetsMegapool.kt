package io.xol.chunkstories.graphics.vulkan.resources

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import org.lwjgl.vulkan.VkCommandBuffer

class DescriptorSetsMegapool(val backend: VulkanGraphicsBackend) : Cleanable {

    inner class ShaderBindingContext private constructor(val frame: Frame, val pipeline: Pipeline, val commandBuffer: VkCommandBuffer) {
        fun bindUBO(interfaceBlock: InterfaceBlock) {

        }

        fun recycle() {

        }
    }

    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}