package xyz.chunkstories.graphics.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.rendergraph.DepthTestingConfiguration
import xyz.chunkstories.api.graphics.rendergraph.PassOutputsDeclaration
import xyz.chunkstories.graphics.vulkan.graph.UsageState
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.getLayoutForStateAndType
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.textures.vulkanFormat
import xyz.chunkstories.graphics.vulkan.util.VkRenderPass
import xyz.chunkstories.graphics.vulkan.util.ensureIs

class RenderPass(val backend: VulkanGraphicsBackend, val pass: VulkanPass) : Cleanable {

    val outputsDeclaration = pass.declaration.outputs
    val depth = pass.declaration.depthTestingConfiguration

    val handle: VkRenderPass

    init {
        stackPush()
        val attachmentDescription = VkAttachmentDescription.callocStack(outputsDeclaration.outputs.size + if (depth.enabled) 1 else 0)
        outputsDeclaration.outputs.mapIndexed { index, output ->
            val renderbuffer = pass.outputRenderBuffers[index]

            val previousUsage = UsageState.OUTPUT//TODO()//renderbuffer.findPreviousUsage()
            val currentUsage = UsageState.OUTPUT

            attachmentDescription[index].apply {

                format(renderbuffer.declaration.format.vulkanFormat.ordinal)
                samples(VK_SAMPLE_COUNT_1_BIT)

                if (output.clear)
                    loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                else {
                    if (previousUsage == UsageState.NONE)
                        loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    else
                        loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                }

                //TODO use DONT_CARE when it can be determined we won't be needing the data
                storeOp(VK_ATTACHMENT_STORE_OP_STORE)

                //TODO we don't even use stencil why is this here
                stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)

                initialLayout(getLayoutForStateAndType(previousUsage, renderbuffer.usageType))
                finalLayout(getLayoutForStateAndType(currentUsage, renderbuffer.usageType))
            }
        }

        if (depth.enabled) {
            val depthBufferAttachmentIndex = attachmentDescription.capacity() - 1
            attachmentDescription[depthBufferAttachmentIndex].apply {
                val renderbuffer = pass.resolvedDepthBuffer!!

                val previousUsage = UsageState.OUTPUT//TODO()//renderbuffer.findPreviousUsage()
                val currentUsage = UsageState.OUTPUT

                format(renderbuffer.declaration.format.vulkanFormat.ordinal)
                samples(VK_SAMPLE_COUNT_1_BIT)

                if (depth.clear)
                    loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                else {
                    if (previousUsage == UsageState.NONE)
                        loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    else
                        loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                }

                //TODO use DONT_CARE when it can be determined we won't be needing the data
                storeOp(VK_ATTACHMENT_STORE_OP_STORE)

                //TODO we don't even use stencil why is this here
                stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)

                initialLayout(getLayoutForStateAndType(previousUsage, renderbuffer.usageType))
                finalLayout(getLayoutForStateAndType(currentUsage, renderbuffer.usageType))
            }
        }

        val colorAttachmentReference = VkAttachmentReference.callocStack(outputsDeclaration.outputs.size)
        outputsDeclaration.outputs.mapIndexed { index, output ->
            colorAttachmentReference[index].apply {
                attachment(index)
                layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                //layout(VK_IMAGE_LAYOUT_GENERAL)
            }
        }

        val subpassDescription = VkSubpassDescription.callocStack(1).apply {
            pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)

            pColorAttachments(colorAttachmentReference)
            colorAttachmentCount(colorAttachmentReference.capacity())

            if (depth.enabled) {
                val depthBufferAttachmentIndex = attachmentDescription.capacity() - 1
                val depthAttachmentReference = VkAttachmentReference.callocStack().apply {
                    attachment(depthBufferAttachmentIndex)
                    if (depth.write)
                        layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                    else
                        layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL)
                }
                pDepthStencilAttachment(depthAttachmentReference)
            }
        }

        val dependencies = VkSubpassDependency.calloc(1).apply {
            srcSubpass(VK_SUBPASS_EXTERNAL)
            dstSubpass(0)

            //TODO we could be really smart here and be aware of the read/writes between passes to further optimize those masks
            //TODO maybe even do different scheduling absed on that. Unfortunately I just want to get this renderer going atm
            //var stages = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
            var access = VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
            if (depth.enabled) {
                access = access or VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT or VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
            }

            if (depth.enabled)
                srcStageMask(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT or VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            else
                srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)

            // If this is the first pass we just want to wait on the image being available
            /*if (graph.passesInOrder.indexOf(this@VulkanPass) == 0)
                srcAccessMask(0)
            else
                srcAccessMask(access)*/
            //TODO

            srcAccessMask(0)

            if (depth.enabled)
                dstStageMask(VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT or VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            else
                dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            dstAccessMask(access)
        }

        val renderPassCreateInfo = VkRenderPassCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO).apply {
            pAttachments(attachmentDescription)
            pSubpasses(subpassDescription)

            pDependencies(dependencies)
        }

        val pRenderPass = MemoryStack.stackMallocLong(1)
        vkCreateRenderPass(backend.logicalDevice.vkDevice, renderPassCreateInfo, null, pRenderPass).ensureIs("Failed to create render pass", VK_SUCCESS)
        handle = pRenderPass.get(0)
        stackPop()
    }

    inner class Subpass(val index: Int) {
        val renderPass: RenderPass
            get() = this@RenderPass
    }

    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}