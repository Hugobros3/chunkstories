package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.graphics.vulkan.util.VkRenderPass
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.slf4j.LoggerFactory

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*

class VulkanRenderPass(val backend: VulkanGraphicsBackend) {
    val handle: VkRenderPass

    init {
        logger.info("Creating render pass")

        stackPush()
        val attachmentDescription = VkAttachmentDescription.callocStack(1).apply {
            format(backend.physicalDevice.swapchainDetails.formatToUse.ordinal)
            samples(VK_SAMPLE_COUNT_1_BIT)

            loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            storeOp(VK_ATTACHMENT_STORE_OP_STORE)

            stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)

            initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
        }

        val colorAttachmentReference = VkAttachmentReference.callocStack(1).apply {
            attachment(0)
            layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
        }

        val subpassDescription = VkSubpassDescription.callocStack(1).apply {
            pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            pColorAttachments(colorAttachmentReference)
            colorAttachmentCount(1)
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

        val pRenderPass = stackMallocLong(1)
        vkCreateRenderPass(backend.logicalDevice.vkDevice, renderPassCreateInfo, null, pRenderPass).ensureIs("Failed to create render pass", VK_SUCCESS)
        handle = pRenderPass.get(0)

        stackPop()
    }

    fun cleanup() {
        vkDestroyRenderPass(backend.logicalDevice.vkDevice, handle, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}