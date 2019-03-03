package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.GraphicSystem
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.RenderPass
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import xyz.chunkstories.graphics.vulkan.util.VkFramebuffer
import xyz.chunkstories.graphics.vulkan.util.ensureIs

open class VulkanPass(val backend: VulkanGraphicsBackend, val renderTask: VulkanRenderTask, val declaration: PassDeclaration) : Cleanable {
    val canonicalRenderPass: RenderPass
    val renderPassesMap = mutableMapOf<List<UsageType>, RenderPass>()

    var drawingSystems: List<VulkanDrawingSystem>
        private set

    var dispatchingDrawers: List<VulkanDispatchingSystem.Drawer<*>>
        private set

    private val commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

    init {
        MemoryStack.stackPush()
        canonicalRenderPass = RenderPass(backend, this, null)
        val drawingSystems = mutableListOf<VulkanDrawingSystem>()
        val dispatchingDrawers = mutableListOf<VulkanDispatchingSystem.Drawer<*>>()

        val rs = declaration.draws?.registeredSystems
        if (rs != null) {
            for (declaredDrawingSystem in rs) {

                if (DrawingSystem::class.java.isAssignableFrom(declaredDrawingSystem.clazz)) {
                    val drawingSystem = backend.createDrawingSystem(this, declaredDrawingSystem as RegisteredGraphicSystem<DrawingSystem>) as VulkanDrawingSystem

                    val d = declaredDrawingSystem.dslCode as GraphicSystem.() -> Unit
                    drawingSystem.apply(d)

                    drawingSystems.add(drawingSystem)
                } else if (DispatchingSystem::class.java.isAssignableFrom(declaredDrawingSystem.clazz)) {
                    val dispatchingSystem = backend.getOrCreateDispatchingSystem(renderTask.renderGraph.dispatchingSystems, declaredDrawingSystem as RegisteredGraphicSystem<DispatchingSystem>)
                    val drawer = dispatchingSystem.createDrawerForPass(this, declaredDrawingSystem.dslCode as VulkanDispatchingSystem.Drawer<*>.() -> Unit)

                    dispatchingSystem.drawersInstances.add(drawer)
                    dispatchingDrawers.add(drawer)
                }
                else
                    throw Exception("What is this")

            }
        }

        this.drawingSystems = drawingSystems
        this.dispatchingDrawers = dispatchingDrawers

        MemoryStack.stackPop()
    }

    fun createFramebuffer(resolvedDepthAndColorBuffers: MutableList<VulkanRenderBuffer>): VkFramebuffer {
        stackPush()
        val pAttachments = stackMallocLong(resolvedDepthAndColorBuffers.size)

        resolvedDepthAndColorBuffers.forEach { renderBuffer -> pAttachments.put(renderBuffer.texture.imageView) }
        pAttachments.flip()

        val viewportSize = resolvedDepthAndColorBuffers[0].size

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

    fun render(frame: Frame,
               passInstance: VulkanFrameGraph.FrameGraphNode.PassNode,
               passInstanceIndex: Int,
               allBufferStates: MutableMap<VulkanRenderBuffer, UsageType>,
               representationsGathered: MutableMap<VulkanDispatchingSystem.Drawer<*>, ArrayList<*>>
    ) {
        val outputs = declaration.outputs.outputs
        val depth = declaration.depthTestingConfiguration

        fun resolveRenderTarget(renderTarget: RenderTarget): VulkanRenderBuffer = when (renderTarget) {
            RenderTarget.BackBuffer -> TODO()
            is RenderTarget.RenderBufferReference -> renderTask.buffers[renderTarget.renderBufferName]
                    ?: throw Exception("Missing render target: No render buffer named '${renderTarget.renderBufferName}' found in RenderTask ${renderTask.declaration.name}")
            is RenderTarget.TaskInput -> (passInstance.context.parameters[renderTarget.name]
                    ?: throw Exception("The parent context lacks a '${renderTarget.name}' parameter")) as? VulkanRenderBuffer
                    ?: throw Exception("The parent context lacks a '${renderTarget.name}' parameter is not a render buffer")
        }

        val resolvedOutputs = mutableMapOf<PassOutput, VulkanRenderBuffer>()

        val resolvedDepthAndColorBuffers = mutableListOf<VulkanRenderBuffer>()
        for (colorOutput in outputs) {
            val resolved = resolveRenderTarget(colorOutput.target ?: RenderTarget.RenderBufferReference(colorOutput.name))
            resolvedDepthAndColorBuffers.add(resolved)
            resolvedOutputs[colorOutput] = resolved
        }
        if (depth.enabled) {
            val resolved = resolveRenderTarget(depth.depthBuffer!!)
            resolvedDepthAndColorBuffers.add(resolved)
            passInstance.resolvedDepthBuffer = resolved
        }

        passInstance.resolvedOutputs = resolvedOutputs

        //TODO also bind textures at that point?
        val resolvedInputBuffers = mutableListOf<VulkanRenderBuffer>()
        for (imageInput in declaration.inputs?.imageInputs ?: emptyList<ImageInput>()) {
            val source = imageInput.source
            when (source) {
                is ImageSource.AssetReference -> {
                }
                is ImageSource.TextureReference -> {
                }
                is ImageSource.RenderBufferReference -> resolvedInputBuffers.add(renderTask.buffers[source.renderBufferName]
                        ?: throw Exception("No renderbuffer named ${source.renderBufferName}"))
                is ImageSource.TaskOutput -> {
                    val referencedContext = source.context as VulkanFrameGraph.FrameGraphNode.RenderingContextNode

                    //TODO we only handle direct deps for now
                    val rootPass = referencedContext.renderTask.rootPass
                    val passInstance = referencedContext.depends.find { it is PassInstance && it.pass == rootPass.declaration } as VulkanFrameGraph.FrameGraphNode.PassNode

                    val outputInstance = passInstance.resolvedOutputs[source.output]!!
                    resolvedInputBuffers.add(outputInstance)
                }
                is ImageSource.TaskOutputDepth -> {
                    val referencedContext = source.context as VulkanFrameGraph.FrameGraphNode.RenderingContextNode

                    //TODO we only handle direct deps for now
                    val rootPass = referencedContext.renderTask.rootPass
                    val passInstance = referencedContext.depends.find { it is PassInstance && it.pass == rootPass.declaration } as VulkanFrameGraph.FrameGraphNode.PassNode

                    val outputInstance = passInstance.resolvedDepthBuffer!!
                    resolvedInputBuffers.add(outputInstance)
                }
            }
        }

        for (image in passInstance.extraInputRenderBuffers) {
            resolvedInputBuffers += image
        }

        val attachementsPreviousState = resolvedDepthAndColorBuffers.map {
            allBufferStates[it] ?: UsageType.NONE
        }
        //println(attachementsPreviousState.mapIndexed { i, u -> "${resolvedDepthAndColorBuffers[i]}:$u"})
        //println("${passInstance.vulkanPass.declaration.name}:${passInstance.context.name} $attachementsPreviousState")

        val relevantRenderPass = renderPassesMap.getOrPut(attachementsPreviousState) {
            RenderPass(backend, this, attachementsPreviousState)
        }

        //TODO relevant framebuffer
        val framebuffer = createFramebuffer(resolvedDepthAndColorBuffers)

        //TODO
        val imageInputstoTransition = mutableListOf<Pair<VulkanRenderBuffer, UsageType>>()
        for (imageInputResolved in resolvedInputBuffers) {
            val currentUsage = allBufferStates[imageInputResolved] ?: UsageType.NONE
            if (currentUsage != UsageType.INPUT)
                imageInputstoTransition.add(Pair(imageInputResolved, currentUsage))
        }

        val viewportSize = resolvedDepthAndColorBuffers[0].size

        val commandBuffer = commandPool.createOneUseCB()
        passInstance.commandBuffer = commandBuffer

        stackPush().use {
            commandBuffer.apply {
                /*val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                    flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    pInheritanceInfo(null)
                }

                vkBeginCommandBuffer(this, beginInfo)*/

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

                if (imageInputstoTransition.size > 0) {
                    stackPush()
                    val imageBarriers = VkImageMemoryBarrier.callocStack(imageInputstoTransition.size)
                    var tightestSrcStageMask: Int = 1
                    var tighestDstStageMask: Int = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT
                    for (index in 0 until imageInputstoTransition.size) {
                        val (renderBuffer, previousUsage) = imageInputstoTransition[index]
                        val newUsage = UsageType.INPUT

                        imageBarriers[index].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                            oldLayout(getLayoutForStateAndType(previousUsage, renderBuffer.attachementType))
                            newLayout(getLayoutForStateAndType(newUsage, renderBuffer.attachementType))

                            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            image(renderBuffer.texture.imageHandle)

                            subresourceRange().apply {
                                aspectMask(renderBuffer.attachementType.aspectMask())
                                baseMipLevel(0)
                                levelCount(1)
                                baseArrayLayer(0)
                                layerCount(1)
                            }

                            srcAccessMask(previousUsage.accessMask())
                            dstAccessMask(newUsage.accessMask())
                        }

                        val srcStageMask = when (previousUsage) {
                            UsageType.INPUT -> VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                            UsageType.OUTPUT -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            UsageType.NONE -> VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
                        }

                        val dstStageMask = when (newUsage) {
                            UsageType.INPUT -> VK_PIPELINE_STAGE_VERTEX_SHADER_BIT
                            UsageType.OUTPUT -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            UsageType.NONE -> VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT
                        }

                        if (srcStageMask > tightestSrcStageMask)
                            tightestSrcStageMask = srcStageMask

                        if (dstStageMask < tighestDstStageMask)
                            tighestDstStageMask = dstStageMask
                    }
                    vkCmdPipelineBarrier(commandBuffer, tightestSrcStageMask, tighestDstStageMask, 0, null, null, imageBarriers)
                    stackPop()
                }

                vkCmdBeginRenderPass(this, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)

                // Transition image layouts now !
                for (drawingSystem in drawingSystems) {
                    drawingSystem.registerDrawingCommands(frame, this, passInstance)
                }

                for(drawer in dispatchingDrawers) {
                    val relevantBucket = representationsGathered[drawer] ?: continue

                    //val filter = 1 shl passInstanceIndex
                    //val filteredByIndex = relevantBucket.representations.asSequence().filterIndexed { i, r -> relevantBucket.visibility[i] and filter != 0 }
                    //drawer.registerDrawingCommands(frame, passInstance, this, filteredByIndex as Sequence<Nothing>)
                    drawer.registerDrawingCommands(frame, passInstance, this, relevantBucket.toList().asSequence() as Sequence<Nothing>)
                }

                vkCmdEndRenderPass(this)
                vkEndCommandBuffer(this)
            }
        }

        for (output in resolvedDepthAndColorBuffers)
            allBufferStates[output] = UsageType.OUTPUT

        for (input in resolvedInputBuffers)
            allBufferStates[input] = UsageType.INPUT

        frame.recyclingTasks.add {
            vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)
            vkFreeCommandBuffers(backend.logicalDevice.vkDevice, commandPool.handle, commandBuffer)
        }
    }

    override fun cleanup() {
        drawingSystems.forEach(Cleanable::cleanup)
        dispatchingDrawers.forEach(Cleanable::cleanup)

        canonicalRenderPass.cleanup()
        for (renderPass in renderPassesMap.values)
            renderPass.cleanup()

        commandPool.cleanup()
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}