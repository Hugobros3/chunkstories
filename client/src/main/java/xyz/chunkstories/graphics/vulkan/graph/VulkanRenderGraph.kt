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
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.swapchain.SwapchainBlitHelper
import xyz.chunkstories.graphics.vulkan.util.ensureIs

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, val dslCode: RenderGraphDeclarationScript) : Cleanable {
    val taskDeclarations: List<RenderTaskDeclaration>
    val tasks: Map<String, VulkanRenderTask>

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
        val graph = FrameGraph(this, mainTask, mainCamera)

        val sequencedGraph = graph.sequenceGraph()

        //println(sequencedGraph)

        val globalStates = mutableMapOf<VulkanRenderBuffer, UsageType>()
        for(i in 0 until sequencedGraph.size) {
            val graphNode = sequencedGraph[i]

            if(graphNode is FrameGraph.FrameGraphNode.PassNode) {
                val pass = graphNode.pass

                val requiredRenderBufferStates = pass.renderBufferUsages
                /*
                /** Contains the old (to transition) layouts of buffers this pass requires */
                val previousRenderBufferStates = requiredRenderBufferStates.keys.associateWith {
                    globalStates[it] ?: UsageType.NONE
                }*/

                /**
                 * The layout transitions and storage/load operations for color/depth attachements are embedded in the RenderPass.
                 * This list will be used to index into a RenderPass cache.
                 */
                val previousRenderPassAttachementStates = mutableListOf<UsageType>()
                if(pass.outputDepthRenderBuffer != null)
                    previousRenderPassAttachementStates.add(globalStates[pass.outputDepthRenderBuffer] ?: UsageType.NONE)
                for(rb in pass.outputColorRenderBuffers)
                    previousRenderPassAttachementStates.add(globalStates[rb] ?: UsageType.NONE)

                /** Lists the image inputs that currently are in the wrong layout */
                val inputRenderBuffersToTransition = mutableListOf<Pair<VulkanRenderBuffer, UsageType>>()
                for(rb in pass.inputRenderBuffers) {
                    val currentState = globalStates[rb] ?: UsageType.NONE
                    if(currentState != UsageType.INPUT)
                        inputRenderBuffersToTransition.add(Pair(rb, currentState))
                }

                //println("Pass ${pass.declaration.name}")
                //println("Used buffers previous state: $previousRenderBufferStates")
                //println("Used buffers required state: $requiredRenderBufferStates")
                //println("Renderpass attachements previous states: $previousRenderPassAttachementStates")
                //println("Input render buffers to transition: $inputRenderBuffersToTransition")

                pass.render(frame, previousRenderPassAttachementStates, inputRenderBuffersToTransition)

                for(entry in requiredRenderBufferStates)
                    globalStates[entry.key] = entry.value
            }

        }

        // Validation also makes it so we output a rendergraph image
        if(fresh && backend.enableValidation) {
            lookIDontCare(graph)
            fresh = false
        }

        val passes = sequencedGraph.mapNotNull { (it as? FrameGraph.FrameGraphNode.PassNode)?.pass }

        stackPush().use {
            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val commandBuffers = MemoryStack.stackMallocPointer(passes.size)
                for (pass in passes)
                    commandBuffers.put(pass.commandBuffers[frame])
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

        blitHelper.copyFinalRenderbuffer(frame, passes.last().outputColorRenderBuffers[0])
    }

    fun resizeBuffers() {
        tasks.values.forEach {
            it.buffers.values.forEach { it.resize() }
            it.passes.values.forEach { it.recreateFramebuffer() }
        }
    }

    override fun cleanup() {
        tasks.values.forEach(Cleanable::cleanup)
        blitHelper.cleanup()
    }
}