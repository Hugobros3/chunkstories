package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.graphics.ImageInput
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.rendergraph.RenderBuffer
import xyz.chunkstories.graphics.common.DummyRenderBuffer
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.slf4j.LoggerFactory
import xyz.chunkstories.graphics.vulkan.util.VkImageLayout

enum class UsageState {
    NONE,
    INPUT,
    OUTPUT
}

enum class UsageType {
    COLOR, DEPTH
}

fun getLayoutForStateAndType(usageState: UsageState, usageType: UsageType) : VkImageLayout = when(usageState) {
    UsageState.NONE -> VK_IMAGE_LAYOUT_UNDEFINED
    UsageState.INPUT -> VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
    UsageState.OUTPUT -> when(usageType) {
        UsageType.COLOR -> VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
        UsageType.DEPTH -> VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL
    }
}

class VulkanRenderBuffer(val backend: VulkanGraphicsBackend, val graph: VulkanRenderGraph, val config: RenderBuffer.() -> Unit) : RenderBuffer(), Cleanable {

    override lateinit var texture: VulkanTexture2D

    /** Set late by the RenderGraph */
    lateinit var layoutPerStage: Map<VulkanPass, Int>

    val usageType: UsageType

    fun UsageType.usageBits() = when(this) {
            UsageType.DEPTH -> VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
            UsageType.COLOR -> VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
        } or VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT // In all cases it'll be sampled and transferred !

    init {
        this.apply(config)

        usageType = when(format) {
            TextureFormat.DEPTH_24, TextureFormat.DEPTH_32 -> UsageType.DEPTH
            else -> UsageType.COLOR
        }

        if(name != "_swapchain")
            texture = VulkanTexture2D(backend, format, size.x, size.y, usageType.usageBits())
        //texture.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL)
        //texture.transitionToRenderBufferLayout()
    }

    fun resize() {
        val scratchBuffer = DummyRenderBuffer().apply(config)
        val newSize = scratchBuffer.size
        if(newSize != size) {
            logger.debug("Resizing render buffer $name")
            size = newSize
            texture.cleanup()
            texture = VulkanTexture2D(backend, format, size.x, size.y, usageType.usageBits())
        }
    }

    fun UsageType.aspectMask() : Int = when(this) {
        UsageType.COLOR -> VK_IMAGE_ASPECT_COLOR_BIT
        UsageType.DEPTH -> VK_IMAGE_ASPECT_DEPTH_BIT
    }

    fun UsageState.accessMask() : Int = when(this) {
        UsageState.NONE -> /** well it's unused duh */ 0
        UsageState.INPUT -> VK_ACCESS_SHADER_READ_BIT

        //TODO if no blend we might not even need the read thing
        UsageState.OUTPUT -> VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
    }

    override fun cleanup() {
        texture.cleanup()
    }

    fun transitionUsage(commandBuffer: VkCommandBuffer, previousUsage: UsageState, newUsage: UsageState) {
        stackPush()
        val imageBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
            oldLayout(getLayoutForStateAndType(previousUsage, usageType))
            newLayout(getLayoutForStateAndType(newUsage, usageType))

            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(texture.imageHandle)

            subresourceRange().apply {
                aspectMask(usageType.aspectMask())
                baseMipLevel(0)
                levelCount(1)
                baseArrayLayer(0)
                layerCount(1)
            }

            srcAccessMask(previousUsage.accessMask())
            dstAccessMask(newUsage.accessMask())
        }

        val srcStageMask = when(previousUsage) {
            UsageState.INPUT -> VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
            UsageState.OUTPUT -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
            UsageState.NONE -> VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
        }

        val dstStageMask = when(newUsage) {
            UsageState.INPUT -> VK_PIPELINE_STAGE_VERTEX_SHADER_BIT
            UsageState.OUTPUT -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
            UsageState.NONE -> VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT
        }

        vkCmdPipelineBarrier(commandBuffer, srcStageMask, dstStageMask, 0, null, null, imageBarrier)
        stackPop()
    }

    fun findUsageInPass(pass: VulkanPass) : UsageState {
        if(pass.outputRenderBuffers.contains(this) || pass.resolvedDepthBuffer == this)
            return UsageState.OUTPUT

        //TODO use resolved variant
        pass.imageInputs.forEach { when(val source = it.source) {
            is ImageInput.ImageSource.RenderBufferReference -> {
                if(source.renderBufferName == this.name)
                    return UsageState.INPUT
            }
        }}

        return UsageState.NONE
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}


