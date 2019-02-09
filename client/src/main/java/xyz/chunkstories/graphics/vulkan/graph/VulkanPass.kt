package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.ImageInput
import xyz.chunkstories.api.graphics.rendergraph.PassDeclaration
import xyz.chunkstories.api.graphics.rendergraph.RenderingContext
import xyz.chunkstories.api.graphics.systems.GraphicSystem
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.RenderPass
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import xyz.chunkstories.graphics.vulkan.util.VkFramebuffer
import xyz.chunkstories.graphics.vulkan.util.ensureIs

open class VulkanPass(val backend: VulkanGraphicsBackend, val renderTask: VulkanRenderTask, val declaration: PassDeclaration) : Cleanable {

    val inputRenderBuffers: List<VulkanRenderBuffer>

    val outputColorRenderBuffers: List<VulkanRenderBuffer>
    val outputDepthRenderBuffer: VulkanRenderBuffer?

    var framebuffer: VkFramebuffer = -1
        internal set

    val canonicalRenderPass: RenderPass
    val renderPassesMap = mutableMapOf<List<UsageType>, RenderPass>()

    val renderBufferUsages: Map<VulkanRenderBuffer, UsageType>

    lateinit var drawingSystems: List<VulkanDrawingSystem>
        private set

    val commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
    val commandBuffers: InflightFrameResource<VkCommandBuffer>

    init {
        MemoryStack.stackPush()

        inputRenderBuffers = declaration.inputs?.imageInputs?.mapNotNull {
            val source = it.source
            if (source is ImageInput.ImageSource.RenderBufferReference) {
                val rb = renderTask.buffers[source.renderBufferName]!!
                rb
            } else
                null
        } ?: emptyList<VulkanRenderBuffer>()

        outputColorRenderBuffers = declaration.outputs.outputs.map { output ->
            val resolvedName = output.outputBuffer ?: output.name
            if (resolvedName == "_swapchain")
                TODO()//graph.dummySwapchainRenderBuffer
            else
                renderTask.buffers[resolvedName] ?: throw Exception("Buffer $resolvedName isn't declared !")
        }

        outputDepthRenderBuffer =
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

        canonicalRenderPass = RenderPass(backend, this, null)
        framebuffer = createFramebuffer()

        renderBufferUsages = createRenderBufferUsageMap(declaration)

        val drawingSystems = mutableListOf<VulkanDrawingSystem>()
        val rs = declaration.draws?.registeredSystems
        if (rs != null) {
            for (declaredDrawingSystem in rs) {
                val drawingSystem = backend.createDrawingSystem(this, declaredDrawingSystem) as VulkanDrawingSystem

                val d = declaredDrawingSystem.dslCode as GraphicSystem.() -> Unit
                drawingSystem.apply(d)
                drawingSystems.add(drawingSystem)
            }
        }

        this.drawingSystems = drawingSystems

        MemoryStack.stackPop()
    }

    private fun createRenderBufferUsageMap(declaration: PassDeclaration): Map<VulkanRenderBuffer, UsageType> {
        val map = mutableMapOf<VulkanRenderBuffer, UsageType>()

        for (inputBuffer in inputRenderBuffers)
            map[inputBuffer] = UsageType.INPUT

        for (outputBuffer in outputColorRenderBuffers) {
            map[outputBuffer] = UsageType.OUTPUT
        }

        if (outputDepthRenderBuffer != null)
            map[outputDepthRenderBuffer] = UsageType.OUTPUT

        return map
    }

    fun getRenderBufferUsages(passNode: FrameGraph.FrameGraphNode.PassNode): Map<VulkanRenderBuffer, UsageType> {
        val map = renderBufferUsages.toMutableMap()
        for (irb in passNode.extraInputRenderBuffers) {
            map[irb] = UsageType.INPUT
        }

        return map
    }

    fun getAllInputRenderBuffers(passNode: FrameGraph.FrameGraphNode.PassNode): List<VulkanRenderBuffer> {
        val list = inputRenderBuffers.toMutableList()
        for (irb in passNode.extraInputRenderBuffers) {
            list.add(irb)
        }

        return list
    }

    fun createFramebuffer(): VkFramebuffer {
        stackPush()
        val pAttachments = stackMallocLong(declaration.outputs.outputs.size + if (declaration.depthTestingConfiguration.enabled) 1 else 0)

        outputColorRenderBuffers.forEach { renderBuffer -> pAttachments.put(renderBuffer.texture.imageView) }
        if (declaration.depthTestingConfiguration.enabled)
            pAttachments.put(outputDepthRenderBuffer!!.texture.imageView)
        pAttachments.flip()

        val viewportSize = (outputDepthRenderBuffer ?: outputColorRenderBuffers[0]!!).size

        val framebufferCreateInfo = VkFramebufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).apply {
            renderPass(canonicalRenderPass.handle)
            pAttachments(pAttachments)
            width(viewportSize.x)
            height(viewportSize.y)
            layers(1)
        }

        val pFramebuffer = stackMallocLong(1)
        vkCreateFramebuffer(backend.logicalDevice.vkDevice, framebufferCreateInfo, null, pFramebuffer).ensureIs("Failed to create framebuffer", VK_SUCCESS)
        val handle = pFramebuffer.get(0)
        stackPop()
        return handle
    }

    fun render(frame: Frame, passInstance: FrameGraph.FrameGraphNode.PassNode, attachementsPreviousState: List<UsageType>, imageInputstoTransition: List<Pair<VulkanRenderBuffer, UsageType>>) {
        val outputs = declaration.outputs.outputs
        val depth = declaration.depthTestingConfiguration

        val viewportSize = (outputDepthRenderBuffer ?: outputColorRenderBuffers[0]!!).size

        val relevantRenderPass = renderPassesMap.getOrPut(attachementsPreviousState) {
            RenderPass(backend, this, attachementsPreviousState)
        }

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
                    width(viewportSize.x.toFloat())
                    height(viewportSize.y.toFloat())
                    minDepth(0.0F)
                    maxDepth(1.0F)
                }

                val zeroZero = VkOffset2D.callocStack().apply {
                    x(0)
                    y(0)
                }
                val scissor = VkRect2D.callocStack(1).apply {
                    offset(zeroZero)
                    extent().width(viewportSize.x)
                    extent().height(viewportSize.y)
                }

                vkCmdSetViewport(this, 0, viewport)
                vkCmdSetScissor(this, 0, scissor)

                val renderPassBeginInfo = VkRenderPassBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).apply {
                    renderPass(relevantRenderPass.handle)
                    framebuffer(framebuffer)

                    renderArea().offset().x(0)
                    renderArea().offset().y(0)
                    renderArea().extent().width(viewportSize.x)
                    renderArea().extent().height(viewportSize.y)

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


                for ((rb, ou) in imageInputstoTransition) {
                    rb.transitionUsage(this, ou, UsageType.INPUT)
                }

                vkCmdBeginRenderPass(this, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)

                // Transition image layouts now !

                for (drawingSystem in drawingSystems) {
                    drawingSystem.registerDrawingCommands(frame, this, passInstance)
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

        vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)

        canonicalRenderPass.cleanup()
        for (renderPass in renderPassesMap.values)
            renderPass.cleanup()

        commandPool.cleanup()
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}