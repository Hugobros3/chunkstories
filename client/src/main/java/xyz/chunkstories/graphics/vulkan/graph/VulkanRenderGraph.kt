package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSubmitInfo
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclaration
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.rendergraph.RenderTaskDeclaration
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.representations.gatherRepresentations
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.swapchain.SwapchainBlitHelper
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.util.ensureIs

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, val dslCode: RenderGraphDeclarationScript) : Cleanable {
    val taskDeclarations: List<RenderTaskDeclaration>
    val tasks: Map<String, VulkanRenderTask>

    val dispatchingSystems = mutableListOf<VulkanDispatchingSystem<*>>()

    val blitHelper = SwapchainBlitHelper(backend)

    var fresh = true

    init {
        taskDeclarations = RenderGraphDeclaration().also(dslCode).renderTasks.values.toList()
        tasks = taskDeclarations.map {
            val vulkanRenderTask = VulkanRenderTask(backend, it)
            Pair(it.name, vulkanRenderTask)
        }.toMap()
    }

    fun renderFrame(frame: Frame) {
        //TODO make that configurable
        val mainCamera = backend.window.client.ingame?.player?.controlledEntity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val mainTaskName = "main"

        val mainTask = tasks[mainTaskName]!!

        //val entity = backend.window.client.ingame?.player?.controlledEntity
        //val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()

        val map = mutableMapOf<String, Any>()
        map["camera"] = mainCamera

        val graph = VulkanFrameGraph(frame, this, mainTask, mainCamera, map)

        val sequencedGraph = graph.sequenceGraph()

        val gathered = backend.graphicsEngine.gatherRepresentations(graph, sequencedGraph)

        var passIndex = 0
        val globalStates = mutableMapOf<VulkanRenderBuffer, UsageType>()
        for(graphNodeIndex in 0 until sequencedGraph.size) {
            val graphNode = sequencedGraph[graphNodeIndex]

            when (graphNode) {
                is VulkanFrameGraph.FrameGraphNode.PassNode -> {
                    val pass = graphNode.vulkanPass

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

                    pass.render(frame, graphNode, passIndex++, globalStates, gathered)

                    /*/** Update the state of the buffers used in that pass */
                    for(entry in requiredRenderBufferStates)
                        globalStates[entry.key] = entry.value*/
                }
                is VulkanFrameGraph.FrameGraphNode.RenderingContextNode -> graphNode.callback?.invoke(graphNode)
            }
        }

        // Validation also makes it so we output a rendergraph image
        if(fresh && backend.enableValidation) {
            exportRenderGraphPng(graph)
            fresh = false
        }

        val passesInstances = sequencedGraph.mapNotNull { (it as? VulkanFrameGraph.FrameGraphNode.PassNode) }
        val vulkanPasses = passesInstances.map { it.vulkanPass }

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

        blitHelper.copyFinalRenderbuffer(frame, passesInstances.last().resolvedOutputs[vulkanPasses.last().declaration.outputs.outputs[0]] !!)
    }

    fun resizeBuffers() {
        tasks.values.forEach {
            it.buffers.values.forEach { it.resize() }
            //it.passes.values.forEach { it.recreateFramebuffer() }
        }
    }

    override fun cleanup() {
        dispatchingSystems.forEach(Cleanable::cleanup)

        tasks.values.forEach(Cleanable::cleanup)
        blitHelper.cleanup()
    }
}