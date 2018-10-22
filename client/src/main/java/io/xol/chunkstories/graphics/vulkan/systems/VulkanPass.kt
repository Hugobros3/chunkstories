package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.graphics.rendergraph.Pass
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.VulkanRenderGraph
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.textures.vulkanFormat
import io.xol.chunkstories.graphics.vulkan.util.VkRenderPass
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class VulkanPass(val backend: VulkanGraphicsBackend, val graph: VulkanRenderGraph, config: Pass.() -> Unit) : Pass(), Cleanable {

    val vkRenderPass : VkRenderPass

    init {
        this.apply(config)

        MemoryStack.stackPush()

        val attachmentDescription = VkAttachmentDescription.callocStack(outputs.size)
        outputs.mapIndexed { index, output ->
            attachmentDescription[index].apply {
                val renderbuffer = graph.buffers[output.outputBuffer] ?: throw Exception("Buffer ${output.outputBuffer} isn't declared !")

                format(renderbuffer.format.vulkanFormat.ordinal)
                samples(VK_SAMPLE_COUNT_1_BIT)

                if(output.clear)
                    loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                else
                    loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                //TODO use DONT_CARE when it can be determined we won't be needing the data
                storeOp(VK_ATTACHMENT_STORE_OP_STORE)

                //TODO we don't even use stencil why is this here
                stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)

                //TODO use a smarter layout
                initialLayout(VK_IMAGE_LAYOUT_GENERAL)
                finalLayout(VK_IMAGE_LAYOUT_GENERAL)
            }
        }

        val colorAttachmentReference = VkAttachmentReference.callocStack(outputs.size)
        outputs.mapIndexed { index, output ->
            colorAttachmentReference[index].apply {
                attachment(index)
                //layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                layout(VK_IMAGE_LAYOUT_GENERAL)
            }
        }

        val subpassDescription = VkSubpassDescription.callocStack(1).apply {
            pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)

            pColorAttachments(colorAttachmentReference)
            colorAttachmentCount(colorAttachmentReference.capacity())
        }

        val dependencies = VkSubpassDependency.calloc(1).apply {
            srcSubpass(VK_SUBPASS_EXTERNAL)
            dstSubpass(0)

            srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            srcAccessMask(0)

            dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
        }

        val renderPassCreateInfo = VkRenderPassCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO).apply {
            pAttachments(attachmentDescription)
            pSubpasses(subpassDescription)

            pDependencies(dependencies)
        }

        val pRenderPass = MemoryStack.stackMallocLong(1)
        vkCreateRenderPass(backend.logicalDevice.vkDevice, renderPassCreateInfo, null, pRenderPass).ensureIs("Failed to create render pass", VK_SUCCESS)
        vkRenderPass = pRenderPass.get(0)

        MemoryStack.stackPop()
    }

    override fun cleanup() {
        vkDestroyRenderPass(backend.logicalDevice.vkDevice, vkRenderPass, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}