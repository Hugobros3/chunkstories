package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSubmitInfo
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.representations.gatherRepresentations
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.swapchain.SwapchainBlitHelper
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.util.ensureIs

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, val dslCode: RenderGraphDeclarationScript) : Cleanable {
    val tasks: Map<String, VulkanRenderTask>

    val commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

    val dispatchingSystems = mutableListOf<VulkanDispatchingSystem<*,*>>()

    val blitHelper = SwapchainBlitHelper(backend)

    var fresh = true

    init {
        tasks = RenderGraphDeclaration().also(dslCode).renderTasks.values.toList().map {
            val vulkanRenderTask = VulkanRenderTask(backend, this, it)
            Pair(it.name, vulkanRenderTask)
        }.toMap()
    }

    fun renderFrame(frame: VulkanFrame) {
        // Gather the base information we need to start rendering
        //TODO make that configurable
        val mainCamera = backend.window.client.ingame?.player?.controlledEntity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val mainTaskName = "main"
        val mainTask = tasks[mainTaskName]!!
        val map = mutableMapOf<String, Any>()
        map["camera"] = mainCamera

        val graph = VulkanFrameGraph(frame, this, mainTask, mainCamera, map)
        val sequencedGraph = graph.sequenceGraph()

        val passInstances: Array<PassInstance> = sequencedGraph.filterIsInstance<PassInstance>().toTypedArray()
        val renderingContexts: Array<RenderTaskInstance> = sequencedGraph.filterIsInstance<RenderTaskInstance>().toTypedArray()
        val gathered = backend.graphicsEngine.gatherRepresentations(frame, passInstances, renderingContexts)

        // Fancy preparing of the representations to render
        val workForDrawersPerPassInstance: List<MutableMap<VulkanDispatchingSystem.Drawer<*>, *>> = passInstances.map {
            mutableMapOf<VulkanDispatchingSystem.Drawer<*>, Any>()
        }

        for ((passInstanceIndex, passInstance) in passInstances.withIndex()) {
            val pass = passInstance as VulkanFrameGraph.FrameGraphNode.VulkanPassInstance

            val renderContextIndex = renderingContexts.indexOf(pass.taskInstance)
            val ctxMask = 1 shl renderContextIndex

            val workForDrawers = workForDrawersPerPassInstance[passInstanceIndex]

            for ((representationName, contents) in gathered.buckets) {
                val responsibleSystem = dispatchingSystems.find { it.representationName == representationName } ?: continue

                val drawers = pass.pass.dispatchingDrawers.filter {
                    it.system == responsibleSystem
                }

                val filteredRepresentations = contents.representations.filterIndexed { index, _ -> contents.masks[index] and ctxMask != 0 }
                (responsibleSystem as VulkanDispatchingSystem<Representation, *>).sort_(filteredRepresentations.asSequence(), drawers, workForDrawers)
            }
        }

        var passIndex = 0
        val globalStates = mutableMapOf<VulkanRenderBuffer, UsageType>()
        for (graphNodeIndex in 0 until sequencedGraph.size) {
            val graphNode = sequencedGraph[graphNodeIndex]

            when (graphNode) {
                is VulkanFrameGraph.FrameGraphNode.VulkanPassInstance -> {
                    val pass = graphNode.pass

                    /*val requiredRenderBufferStates = pass.getRenderBufferUsages(graphNode)

                    /**
                     * The layout transitions and storage/load operations for color/depth attachements are embedded in the RenderPass.
                     * This list will be used to index into a RenderPass cache.
                     */
                    val previousAttachementStates = mutableListOf<UsageType>()
                    if(pass.outputDepthRenderBuffer != null)
                        previousAttachementStates.add(globalStates[pass.outputDepthRenderBuffer] ?: UsageType.NONE)
                    for(rb in pass.outputColorRenderBuffers)
                        previousAttachementStates.add(globalStates[rb] ?: UsageType.NONE)

                    /** Lists the image inputs that currently are in the wrong layout */
                    val inputRenderBuffersToTransition = mutableListOf<Pair<VulkanRenderBuffer, UsageType>>()
                    for(rb in pass.getAllInputRenderBuffers(graphNode)) {
                        val currentState = globalStates[rb] ?: UsageType.NONE
                        if(currentState != UsageType.INPUT)
                            inputRenderBuffersToTransition.add(Pair(rb, currentState))
                    }*/

                    //println("ctx=${graphNode.context.name} ${pass.declaration.name} jobs[$passIndex]=${jobs[passIndex].mapValues { it.value.size }}")
                    pass.render(frame, graphNode, globalStates, workForDrawersPerPassInstance[passIndex])

                    passIndex++
                    /*/** Update the state of the buffers used in that pass */
                    for(entry in requiredRenderBufferStates)
                        globalStates[entry.key] = entry.value*/
                }
                is VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance -> {
                    graphNode.callbacks.forEach { it.invoke(graphNode) }
                }
            }
        }

        // Validation also makes it so we output a rendergraph image
        if (fresh && backend.enableValidation) {
            exportRenderGraphPng(graph)
            fresh = false
        }

        val passesInstances = sequencedGraph.mapNotNull { (it as? VulkanFrameGraph.FrameGraphNode.VulkanPassInstance) }
        val vulkanPasses = passesInstances.map { it.pass }

        //println(passesInstances.map { "${it.vulkanPass.declaration.name}:${it.context.name}" })

        stackPush().use {
            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val commandBuffers = MemoryStack.stackMallocPointer(passesInstances.size)
                for (passInstance in passesInstances)
                    commandBuffers.put(passInstance.commandBuffer)

                commandBuffers.flip()
                pCommandBuffers(commandBuffers)

                val waitOnSemaphores = MemoryStack.stackMallocLong(1)
                waitOnSemaphores.put(0, frame.renderCanBeginSemaphore)
                pWaitSemaphores(waitOnSemaphores)
                waitSemaphoreCount(1)

                val waitStages = MemoryStack.stackMallocInt(1)
                waitStages.put(0, VK_PIPELINE_STAGE_TRANSFER_BIT)
                pWaitDstStageMask(waitStages)

                //val semaphoresToSignal = MemoryStack.stackLongs(frame.renderFinishedSemaphore)
                //pSignalSemaphores(semaphoresToSignal)
            }

            backend.logicalDevice.graphicsQueue.mutex.acquireUninterruptibly()
            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, VK_NULL_HANDLE).ensureIs("Failed to submit command buffer", VK_SUCCESS)
            backend.logicalDevice.graphicsQueue.mutex.release()
        }

        blitHelper.copyFinalRenderbuffer(frame, passesInstances.last().resolvedOutputs[vulkanPasses.last().declaration.outputs.outputs[0]]!!)
    }

    fun resizeBuffers() {
        tasks.values.forEach {
            it.buffers.values.forEach { it.resize() }
            it.passes.values.forEach { it.dumpFramebuffers() }
        }
    }

    override fun cleanup() {
        dispatchingSystems.forEach(Cleanable::cleanup)
        tasks.values.forEach(Cleanable::cleanup)
        blitHelper.cleanup()

        commandPool.cleanup()
    }
}