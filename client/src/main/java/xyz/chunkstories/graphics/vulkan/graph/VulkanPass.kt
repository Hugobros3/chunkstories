package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.RenderPass
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import xyz.chunkstories.graphics.vulkan.util.VkFramebuffer
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import kotlin.collections.ArrayList

open class VulkanPass(val backend: VulkanGraphicsBackend, val renderTask: VulkanRenderTask, val declaration: PassDeclaration) : Cleanable {
    val drawingSystems: List<VulkanDrawingSystem>
    val dispatchingDrawers: List<VulkanDispatchingSystem.Drawer<*>>

    val canonicalRenderPass: RenderPass
    val renderPassesMap = mutableMapOf<List<UsageType>, RenderPass>()

    val frameBuffers = mutableMapOf<List<VulkanRenderBuffer>, VkFramebuffer>()

    init {
        stackPush()
        canonicalRenderPass = RenderPass(backend, this, null)
        drawingSystems = mutableListOf()
        dispatchingDrawers = mutableListOf()

        declaration.draws?.registeredSystems?.let {
            for (registeredSystem in it) {

                if (DrawingSystem::class.java.isAssignableFrom(registeredSystem.clazz)) {
                    val drawingSystem = backend.createDrawingSystem(this, registeredSystem as RegisteredGraphicSystem<DrawingSystem>) as VulkanDrawingSystem

                    //val d = declaredDrawingSystem.dslCode as GraphicSystem.() -> Unit
                    //drawingSystem.apply(d)

                    drawingSystems.add(drawingSystem)
                } else if (DispatchingSystem::class.java.isAssignableFrom(registeredSystem.clazz)) {
                    val dispatchingSystem = backend.getOrCreateDispatchingSystem(renderTask.renderGraph.dispatchingSystems, registeredSystem as RegisteredGraphicSystem<DispatchingSystem>)
                    val drawer = dispatchingSystem.createDrawerForPass(this, registeredSystem.dslCode as VulkanDispatchingSystem.Drawer<*>.() -> Unit)

                    dispatchingSystem.drawersInstances.add(drawer)
                    dispatchingDrawers.add(drawer)
                } else {
                    throw Exception("What is this :$registeredSystem ?")
                }

            }
        }

        stackPop()
    }

    fun render(frame: VulkanFrame,
               passInstance: VulkanFrameGraph.FrameGraphNode.VulkanPassInstance,
               allBufferStates: MutableMap<VulkanRenderBuffer, UsageType>,
               representationsGathered: MutableMap<VulkanDispatchingSystem.Drawer<*>, ArrayList<*>>
    ) {
        declaration.setupLambdas.forEach { it.invoke(passInstance) }

        val outputs = declaration.outputs.outputs
        val depth = declaration.depthTestingConfiguration

        fun resolveRenderTarget(renderTarget: RenderTarget): VulkanRenderBuffer = when (renderTarget) {
            RenderTarget.BackBuffer -> TODO()
            is RenderTarget.RenderBufferReference -> renderTask.buffers[renderTarget.renderBufferName]
                    ?: throw Exception("Missing render target: No render buffer named '${renderTarget.renderBufferName}' found in RenderTask ${renderTask.declaration.name}")
            is RenderTarget.TaskInput -> {
                val resolvedParameter = (passInstance.taskInstance.parameters[renderTarget.name]
                        ?: throw Exception("The parent context lacks a '${renderTarget.name}' parameter"))

                when (resolvedParameter) {
                    is VulkanRenderBuffer -> resolvedParameter
                    is RenderTarget.RenderBufferReference -> {
                        val localRenderBuffer = renderTask.buffers[resolvedParameter.renderBufferName]
                        if (localRenderBuffer != null)
                            localRenderBuffer
                        else {
                            val parentRenderTask = passInstance.taskInstance.requester!!.taskInstance
                            parentRenderTask.renderTask.buffers[resolvedParameter.renderBufferName]
                                    ?: throw Exception("Can't find render buffer named: ${resolvedParameter.renderBufferName}")
                        }
                    }
                    else -> throw Exception("The $resolvedParameter parameter is not a render buffer")
                }
            }
        }

        val resolvedOutputs = mutableMapOf<PassOutput, VulkanRenderBuffer>()

        val resolvedDepthAndColorBuffers = mutableListOf<VulkanRenderBuffer>()
        for (colorOutput in outputs) {
            val resolved = resolveRenderTarget(colorOutput.target
                    ?: RenderTarget.RenderBufferReference(colorOutput.name))
            resolvedDepthAndColorBuffers.add(resolved)
            resolvedOutputs[colorOutput] = resolved
        }
        if (depth.enabled) {
            val resolved = resolveRenderTarget(depth.depthBuffer!!)
            resolvedDepthAndColorBuffers.add(resolved)
            passInstance.resolvedDepthBuffer = resolved
        }

        passInstance.resolvedOutputs = resolvedOutputs

        passInstance.postResolve(resolvedDepthAndColorBuffers)

        val resolvedInputBuffers = mutableListOf<VulkanRenderBuffer>()

        fun lookForRenderBufferImages(list: List<Triple<Any, Any, ImageInput>>) {
            for ((_, _, imageInput) in list) {
                val source = imageInput.source
                when (source) {
                    is ImageSource.AssetReference -> {
                    }
                    is ImageSource.TextureReference -> {
                    }
                    is ImageSource.RenderBufferReference -> {
                        resolvedInputBuffers.add(renderTask.buffers[source.renderBufferName]
                                ?: throw Exception("No renderbuffer named ${source.renderBufferName}"))
                    }
                    is ImageSource.TaskOutput -> {
                        val referencedTaskInstance = source.context as VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance

                        //TODO we only handle direct deps for now
                        val rootPass = referencedTaskInstance.renderTask.rootPass
                        val passInstance = referencedTaskInstance.dependencies.find { it is PassInstance && it.declaration == rootPass.declaration } as VulkanFrameGraph.FrameGraphNode.VulkanPassInstance

                        val outputInstance = passInstance.resolvedOutputs[source.output]!!
                        resolvedInputBuffers.add(outputInstance)
                    }
                    is ImageSource.TaskOutputDepth -> {
                        val referencedTaskInstance = source.context as VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance

                        //TODO we only handle direct deps for now
                        val rootPass = referencedTaskInstance.renderTask.rootPass
                        val passInstance = referencedTaskInstance.dependencies.find { it is PassInstance && it.declaration == rootPass.declaration } as VulkanFrameGraph.FrameGraphNode.VulkanPassInstance

                        val outputInstance = passInstance.resolvedDepthBuffer!!
                        resolvedInputBuffers.add(outputInstance)
                    }
                }
            }
        }
        lookForRenderBufferImages(passInstance.shaderResources.images)
        for(subsystem in passInstance.preparedDispatchingSystemsContexts) {
            lookForRenderBufferImages(subsystem.shaderResources.images)
        }
        for(subsystem in passInstance.preparedDrawingSystemsContexts) {
            lookForRenderBufferImages(subsystem.shaderResources.images)
        }

        val attachementsPreviousState = resolvedDepthAndColorBuffers.map {
            allBufferStates[it] ?: UsageType.NONE
        }
        //println(attachementsPreviousState.mapIndexed { i, u -> "${resolvedDepthAndColorBuffers[i]}:$u"})
        //println("${passInstance.vulkanPass.declaration.name}:${passInstance.context.name} $attachementsPreviousState")

        val relevantRenderPass = renderPassesMap.getOrPut(attachementsPreviousState) {
            RenderPass(backend, this, attachementsPreviousState)
        }

        val framebuffer = frameBuffers.getOrPut(resolvedDepthAndColorBuffers) {
            backend.createFramebuffer(this, resolvedDepthAndColorBuffers)
        }

        /** The images to transition using image barriers, with their current usage/layout */
        val inputImageNeedingLayoutTransition = mutableListOf<Pair<VulkanRenderBuffer, UsageType>>()

        for (imageInputResolved in resolvedInputBuffers) {
            val currentUsage = allBufferStates[imageInputResolved] ?: UsageType.NONE
            if (currentUsage != UsageType.INPUT)
                inputImageNeedingLayoutTransition.add(Pair(imageInputResolved, currentUsage))
        }

        val viewportSize = resolvedDepthAndColorBuffers[0].textureSize

        //val commandBuffer = commandPool.createOneUseCB()
        val commandBuffer = renderTask.renderGraph.commandPool.loanCommandBuffer()
        passInstance.commandBuffer = commandBuffer

        stackPush().use {
            commandBuffer.apply {
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

                if (inputImageNeedingLayoutTransition.size > 0) {
                    stackPush()
                    val imageBarriers = VkImageMemoryBarrier.callocStack(inputImageNeedingLayoutTransition.size)
                    var tightestSrcStageMask: Int = 1
                    var tighestDstStageMask: Int = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT
                    for (index in 0 until inputImageNeedingLayoutTransition.size) {
                        val (renderBuffer, previousUsage) = inputImageNeedingLayoutTransition[index]
                        val newUsage = UsageType.INPUT

                        imageBarriers[index].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                            oldLayout(getLayoutForStateAndType(previousUsage, renderBuffer.attachementType))
                            newLayout(getLayoutForStateAndType(newUsage, renderBuffer.attachementType))

                            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            image(renderBuffer.getRenderToTexture().imageHandle)

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
                for ((i, drawingSystem) in drawingSystems.withIndex()) {
                    drawingSystem.registerDrawingCommands(frame, passInstance.preparedDrawingSystemsContexts[i], this)
                }

                for ((i, drawer) in dispatchingDrawers.withIndex()) {
                    val relevantBucket = representationsGathered[drawer] ?: continue

                    //val filter = 1 shl passInstanceIndex
                    //val filteredByIndex = relevantBucket.representations.asSequence().filterIndexed { i, r -> relevantBucket.visibility[i] and filter != 0 }
                    //drawer.registerDrawingCommands(frame, passInstance, this, filteredByIndex as Sequence<Nothing>)
                    drawer.registerDrawingCommands(frame, passInstance.preparedDispatchingSystemsContexts[i], this, relevantBucket.toList().asSequence() as Sequence<Nothing>)
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
            //vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)
            //vkFreeCommandBuffers(backend.logicalDevice.vkDevice, commandPool.handle, commandBuffer)
            renderTask.renderGraph.commandPool.returnCommandBuffer(commandBuffer)
        }
    }

    fun dumpFramebuffers() {
        frameBuffers.values.forEach {
            vkDestroyFramebuffer(backend.logicalDevice.vkDevice, it, null)
        }
        frameBuffers.clear()
    }

    override fun cleanup() {
        dumpFramebuffers()

        drawingSystems.forEach(Cleanable::cleanup)
        dispatchingDrawers.forEach(Cleanable::cleanup)

        canonicalRenderPass.cleanup()
        for (renderPass in renderPassesMap.values)
            renderPass.cleanup()
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }

    private fun VulkanGraphicsBackend.createFramebuffer(vulkanPass: VulkanPass, renderBuffers: List<VulkanRenderBuffer>): VkFramebuffer {
        stackPush()
        val pAttachments = stackMallocLong(renderBuffers.size)

        renderBuffers.forEach { renderBuffer -> pAttachments.put(renderBuffer.getRenderToTexture().imageView) }
        pAttachments.flip()

        val viewportSize = renderBuffers.first().textureSize

        val framebufferCreateInfo = VkFramebufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).apply {
            renderPass(vulkanPass.canonicalRenderPass.handle)
            pAttachments(pAttachments)
            width(viewportSize.x)
            height(viewportSize.y)
            layers(1)
        }

        val pFramebuffer = stackMallocLong(1)
        vkCreateFramebuffer(logicalDevice.vkDevice, framebufferCreateInfo, null, pFramebuffer).ensureIs("Failed to create framebuffer", VK_SUCCESS)
        val handle = pFramebuffer.get(0)
        stackPop()
        return handle
    }
}