package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.ImageInput
import xyz.chunkstories.api.graphics.rendergraph.PassDeclaration
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.RenderPass
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import xyz.chunkstories.graphics.vulkan.textures.vulkanFormat
import xyz.chunkstories.graphics.vulkan.util.VkFramebuffer
import xyz.chunkstories.graphics.vulkan.util.VkRenderPass
import xyz.chunkstories.graphics.vulkan.util.VkSemaphore
import xyz.chunkstories.graphics.vulkan.util.ensureIs

open class VulkanPass(val backend: VulkanGraphicsBackend, val renderTask: VulkanRenderTask, val declaration: PassDeclaration) : Cleanable {

    val outputRenderBuffers: List<VulkanRenderBuffer>
    val resolvedDepthBuffer: VulkanRenderBuffer?

    //val program: VulkanShaderProgram

    var renderPass: VkRenderPass = -1
        private set
    var framebuffer: VkFramebuffer = -1
        internal set

    val renderpass: RenderPass

    lateinit var drawingSystems: List<VulkanDrawingSystem>
        private set

    val commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
    val commandBuffers: InflightFrameResource<VkCommandBuffer>

    /** Set late by the RenderGraph */
    lateinit var passDependencies: List<VulkanPass>

    init {
        MemoryStack.stackPush()

        outputRenderBuffers = declaration.outputs.outputs.map { output ->
            val resolvedName = output.outputBuffer ?: output.name
            if (resolvedName == "_swapchain")
                TODO()//graph.dummySwapchainRenderBuffer
            else
                renderTask.buffers[resolvedName] ?: throw Exception("Buffer $resolvedName isn't declared !")
        }

        resolvedDepthBuffer =
                if (declaration.depthTestingConfiguration.enabled)
                    renderTask.buffers[declaration.depthTestingConfiguration.depthBuffer]
                else null

        commandBuffers = InflightFrameResource(backend) {
            val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
                commandPool(commandPool.handle)
                level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                commandBufferCount(1)
            }

            val pCmdBuffers = MemoryStack.stackMallocPointer(1)
            vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, commandBufferAllocateInfo, pCmdBuffers)

            val commandBuffer = VkCommandBuffer(pCmdBuffers.get(0), backend.logicalDevice.vkDevice)

            commandBuffer
        }

        renderpass = RenderPass(backend, this)

        MemoryStack.stackPop()
    }

    /** We need to know how the graph is laid out to feed Vulkan enough intel */
    fun postGraphBuild() {
        MemoryStack.stackPush()
        renderPass = createRenderPass()
        framebuffer = createFramebuffer()

        /*val drawingSystems = mutableListOf<VulkanDrawingSystem>()
        for (declaredDrawingSystem in declaration.draws.registeredSystems) {
            val drawingSystem = backend.createDrawingSystem(this, declaredDrawingSystem)
            drawingSystem.apply(declaredDrawingSystem.init)
            drawingSystems.add(drawingSystem)
        }

        this.drawingSystems = drawingSystems*/
        TODO()
        MemoryStack.stackPop()
    }

    fun VulkanRenderBuffer.findPreviousUsage(): UsageState {
        TODO()
        /*val thisPassIndex = graph.passesInOrder.indexOf(this@VulkanPass)
        if (thisPassIndex == 0)
            return UsageState.NONE

        //Check all the previous pass to look for a previous usage
        for (index in (thisPassIndex - 1) downTo 0) {
            val usageThere = findUsageInPass(graph.passesInOrder[index])

            if (usageThere != UsageState.NONE)
                return usageThere
        }

        return UsageState.NONE*/
    }

    internal open fun createRenderPass(): VkRenderPass {
        val outputs = declaration.outputs.outputs
        val depth = declaration.depthTestingConfiguration

        val attachmentDescription = VkAttachmentDescription.callocStack(outputs.size + if (depth.enabled) 1 else 0)
        outputs.mapIndexed { index, output ->
            val renderbuffer = outputRenderBuffers[index]

            val previousUsage = renderbuffer.findPreviousUsage()
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
                val renderbuffer = resolvedDepthBuffer!!

                val previousUsage = renderbuffer.findPreviousUsage()
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

        val colorAttachmentReference = VkAttachmentReference.callocStack(outputs.size)
        outputs.mapIndexed { index, output ->
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
        return pRenderPass.get(0)
    }

    internal open fun createFramebuffer(): VkFramebuffer {
        stackPush()
        val pAttachments = stackMallocLong(declaration.outputs.outputs.size + if (declaration.depthTestingConfiguration.enabled) 1 else 0)

        outputRenderBuffers.forEach { renderBuffer -> pAttachments.put(renderBuffer.texture.imageView) }
        if (declaration.depthTestingConfiguration.enabled)
            pAttachments.put(resolvedDepthBuffer!!.texture.imageView)
        pAttachments.flip()

        val framebufferCreateInfo = VkFramebufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).apply {
            renderPass(renderPass)
            pAttachments(pAttachments)
            width(backend.window.width)
            height(backend.window.height)
            layers(1)
        }

        val pFramebuffer = stackMallocLong(1)
        vkCreateFramebuffer(backend.logicalDevice.vkDevice, framebufferCreateInfo, null, pFramebuffer).ensureIs("Failed to create framebuffer", VK_SUCCESS)
        val handle = pFramebuffer.get(0)
        stackPop()
        return handle
    }

    //TODO for now let's assume there is only one pass so we can use head/tail semaphores from the frame object
    open fun render(frame: Frame, passBeginSemaphore: VkSemaphore?) {
        val outputs = declaration.outputs.outputs
        val depth = declaration.depthTestingConfiguration

        stackPush().use {
            commandBuffers[frame].apply {
                val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                    flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    pInheritanceInfo(null)
                }

                vkBeginCommandBuffer(this, beginInfo)

                val viewport = VkViewport.callocStack(1).apply {
                    x(0.0F)
                    y(0.0F)
                    width(backend.window.width.toFloat())
                    height(backend.window.height.toFloat())
                    minDepth(0.0F)
                    maxDepth(1.0F)
                }

                val zeroZero = VkOffset2D.callocStack().apply {
                    x(0)
                    y(0)
                }
                val scissor = VkRect2D.callocStack(1).apply {
                    offset(zeroZero)
                    extent().width(backend.window.width)
                    extent().height(backend.window.height)
                }

                vkCmdSetViewport(this, 0, viewport)
                vkCmdSetScissor(this, 0, scissor)

                val renderPassBeginInfo = VkRenderPassBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).apply {
                    renderPass(renderPass)
                    framebuffer(framebuffer)

                    renderArea().offset().x(0)
                    renderArea().offset().y(0)
                    renderArea().extent().width(backend.window.width)
                    renderArea().extent().height(backend.window.height)

                    val clearColor = VkClearValue.callocStack(outputs.size + if (depth.enabled) 1 else 0)

                    (0 until outputs.size).map { clearColor[it] }.forEachIndexed { i, cc ->
                        cc.color().float32().apply {
                            val clearColor = outputs[i].clearColor
                            this.put(0, clearColor.x().toFloat())
                            this.put(1, clearColor.y().toFloat())
                            this.put(2, clearColor.z().toFloat())
                            this.put(3, clearColor.w().toFloat())
                        }
                    }

                    if (depth.enabled) {
                        val depthBufferAttachmentIndex = outputs.size
                        clearColor[depthBufferAttachmentIndex].let {
                            it.depthStencil().depth(1f)
                        }
                    }

                    pClearValues(clearColor)
                }

                TODO()
                /*imageInputs.forEach { input ->
                    val source = input.source

                    when (source) {
                        is ImageInput.ImageSource.RenderBufferReference -> {
                            val renderBuffer = graph.buffers[source.renderBufferName] ?: throw Exception("Couldn't find render buffer ${input.name}")

                            val previousUsage = renderBuffer.findPreviousUsage()
                            val currentUsage = UsageState.INPUT

                            renderBuffer.transitionUsage(this, previousUsage, currentUsage)
                        }
                    }
                }*/

                vkCmdBeginRenderPass(this, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)

                // Transition image layouts now !

                for (drawingSystem in drawingSystems) {
                    drawingSystem.registerDrawingCommands(frame, this)
                }

                vkCmdEndRenderPass(this)
                vkEndCommandBuffer(this)
            }
        }
    }

    fun recreateFramebuffer() {
        vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)
        framebuffer = createFramebuffer()
    }

    override fun cleanup() {
        drawingSystems.forEach(Cleanable::cleanup)

        // Presentation pass doesn't get to destroy it's resources.
        vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)
        vkDestroyRenderPass(backend.logicalDevice.vkDevice, renderPass, null)

        renderpass.cleanup()

        commandPool.cleanup()
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}