package xyz.chunkstories.graphics.vulkan.util

import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.slf4j.LoggerFactory

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*

/** Contains a bunch of helpers for moving the verboseness of renderpass creation out of the logic */
object RenderPassHelpers {

    fun createWindowSurfaceRenderPass(backend: VulkanGraphicsBackend): VkRenderPass {
        logger.info("Creating render pass")

        stackPush()
        val attachmentDescription = VkAttachmentDescription.callocStack(1).also {
            it.format(backend.physicalDevice.swapchainDetails.formatToUse.ordinal)
            it.samples(VK_SAMPLE_COUNT_1_BIT)

            //loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            it.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            it.storeOp(VK_ATTACHMENT_STORE_OP_STORE)

            it.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            it.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)

            it.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            it.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
        }

        val colorAttachmentReference = VkAttachmentReference.callocStack(1).also {
            it.attachment(0)
            it.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
        }

        val subpassDescription = VkSubpassDescription.callocStack(1).also {
            it.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            it.pColorAttachments(colorAttachmentReference)
            it.colorAttachmentCount(1)
        }

        val dependencies = VkSubpassDependency.callocStack(1).also {
            it.srcSubpass(VK_SUBPASS_EXTERNAL)
            it.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            it.srcAccessMask(0)

            it.dstSubpass(0)
            it.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            it.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
        }

        val renderPassCreateInfo = VkRenderPassCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO).apply {
            pAttachments(attachmentDescription)
            pSubpasses(subpassDescription)

            pDependencies(dependencies)
        }

        val pRenderPass = stackMallocLong(1)
        vkCreateRenderPass(backend.logicalDevice.vkDevice, renderPassCreateInfo, null, pRenderPass).ensureIs("Failed to create render pass", VK_SUCCESS)
        val handle = pRenderPass.get(0)

        stackPop()

        return handle
    }

    val logger = LoggerFactory.getLogger("client.vulkan")!!
}