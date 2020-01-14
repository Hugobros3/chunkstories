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

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, val dslCode: RenderGraphDeclaration.() -> Unit) : Cleanable {
    val declaration = RenderGraphDeclaration().also(dslCode)
    val tasks: Map<String, VulkanRenderTask>

    val commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

    val dispatchingSystems = mutableListOf<VulkanDispatchingSystem<*,*>>()

    val blitHelper = SwapchainBlitHelper(backend)

    var fresh = true

    init {
        tasks = declaration.renderTasks.values.toList().map {
            val vulkanRenderTask = VulkanRenderTask(backend, this, it)
            Pair(it.name, vulkanRenderTask)
        }.toMap()
    }

    fun renderFrame(frame: VulkanFrame) {
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
            val pass = passInstance as VulkanPassInstance

            val renderContextIndex = renderingContexts.indexOf(pass.taskInstance)
            val ctxMask = 1 shl renderContextIndex

            val workForDrawers = workForDrawersPerPassInstance[passInstanceIndex]

            for (bucket in gathered.buckets.values) {
                val responsibleSystem = dispatchingSystems.find { it.representationName == bucket.representationName } ?: continue

                val drawers = pass.pass.dispatchingDrawers.filter {
                    it.system == responsibleSystem
                }

                val filteredRepresentations = bucket.representations.filterIndexed { index, _ -> bucket.masks[index] and ctxMask != 0 }
                (responsibleSystem as VulkanDispatchingSystem<Representation, *>).sort_(filteredRepresentations.asSequence(), drawers, workForDrawers)
            }
        }

        var passIndex = 0
        val globalStates = mutableMapOf<VulkanRenderBuffer, UsageType>()
        for (graphNodeIndex in 0 until sequencedGraph.size) {
            val graphNode = sequencedGraph[graphNodeIndex]

            when (graphNode) {
                is VulkanPassInstance -> {
                    val pass = graphNode.pass
                    pass.render(frame, graphNode, globalStates, workForDrawersPerPassInstance[passIndex])
                    passIndex++
                }
                is VulkanRenderTaskInstance -> {
                    graphNode.callbacks.forEach { it.invoke(graphNode) }
                }
            }
        }

        // Validation also makes it so we output a rendergraph image
        if (fresh && backend.enableValidation) {
            exportRenderGraphPng(graph)
            fresh = false
        }

        val passesInstances = sequencedGraph.mapNotNull { (it as? VulkanPassInstance) }
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