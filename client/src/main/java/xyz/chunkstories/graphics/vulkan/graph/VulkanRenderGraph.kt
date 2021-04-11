package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo
import org.lwjgl.vulkan.VkSubmitInfo
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclaration
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.camera
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.representations.gatherRepresentations
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.debug.exportRenderGraphPng
import xyz.chunkstories.graphics.vulkan.swapchain.SwapchainBlitHelper
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.dispatching.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.util.ensureIs

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, val dslCode: RenderGraphDeclaration.() -> Unit) : Cleanable {
    val declaration = RenderGraphDeclaration().also(dslCode)
    val tasks: Map<String, VulkanRenderTask>

    val commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

    val dispatchingSystems = mutableListOf<VulkanDispatchingSystem<*>>()

    val blitHelper = SwapchainBlitHelper(backend)

    var fresh = true

    init {
        tasks = declaration.renderTasks.values.toList().map {
            val vulkanRenderTask = VulkanRenderTask(backend, this, it)
            Pair(it.name, vulkanRenderTask)
        }.toMap()
    }

    fun renderFrame(frame: VulkanFrame) {
        val mainCamera = backend.window.client.ingame?.camera ?: Camera()
        val mainTaskName = "main"
        val mainTask = tasks[mainTaskName]!!
        val map = mutableMapOf<String, Any>()
        map["camera"] = mainCamera

        val graph = VulkanFrameGraph(frame, this, mainTask, mainCamera, map)
        val sequencedGraph = graph.sequenceGraph()

        val passInstances: List<VulkanPassInstance> = sequencedGraph.filterIsInstance<VulkanPassInstance>()
        val renderTasks: List<VulkanRenderTaskInstance> = sequencedGraph.filterIsInstance<VulkanRenderTaskInstance>()

        renderTasks.forEachIndexed { index, vulkanRenderTaskInstance ->
            vulkanRenderTaskInstance.inOrderId = index
            vulkanRenderTaskInstance.mask = 1 shl index
        }

        passInstances.forEach {
            it.pass.resolveOutputs(it)
        }

        val gathered = backend.graphicsEngine.gatherRepresentations(frame, passInstances, renderTasks)

        val l = passInstances.flatMap { passInstance -> passInstance.pass.dispatchingDrawers.map { drawer -> Pair(passInstance.taskInstance, Pair(passInstance, drawer)) } }
        val l2 = l.groupBy { it.first }.mapValues { it.value.map { it.second } }
        //val l3 = l.groupBy { it.second }.mapValues { it.value.map { it.first } }

        val prepareDrawerCmdBuffers = mutableMapOf<VulkanPassInstance, MutableMap<VulkanDispatchingSystem.Drawer, VkCommandBuffer>>()

        for ((reprClass, metaBucket) in gathered.buckets) {
            // The system that's registered as interested in those representations (only 1:1 for now)
            val responsibleSystem = dispatchingSystems.find { it.representationName == metaBucket.representationName } ?: continue

            val l22 = l2.mapValues { it.value.filter { responsibleSystem.drawersInstances.contains(it.second) } }

            val sortedWork = responsibleSystem.sortWork(frame, l22, metaBucket.maskedBuckets)

            // Get rid of the work for the drawers that won't do anything
            val filteredWork = sortedWork.filter {
                !it.value.isEmpty()
            }

            for (workForDrawer in filteredWork.values) {
                val cmdBuf = backend.renderGraph.commandPool.loanSecondaryCommandBuffer()
                workForDrawer.cmdBuffer = cmdBuf

                stackPush().use {
                    val inheritInfo = VkCommandBufferInheritanceInfo.callocStack().apply {
                        sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
                        renderPass(workForDrawer.drawerInstance.first.pass.canonicalRenderPass.handle)
                        subpass(0)
                        framebuffer(VK_NULL_HANDLE
                                /** I don't know, I mean I could but I can't be arsed :P */)
                    }
                    val beginInfo = VkCommandBufferBeginInfo.callocStack().apply {
                        sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                        flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT or VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
                        pInheritanceInfo(inheritInfo)
                    }
                    vkBeginCommandBuffer(cmdBuf, beginInfo)
                    workForDrawer.drawerInstance.first.pass.setScissorAndViewport(cmdBuf, workForDrawer.drawerInstance.first.renderTargetSize)
                    workForDrawer.drawerInstance.second.registerDrawingCommands(workForDrawer)
                    vkEndCommandBuffer(cmdBuf)
                }
            }

            frame.recyclingTasks += {
                filteredWork.forEach {
                    backend.renderGraph.commandPool.returnSecondaryCommandBuffer(it.value.cmdBuffer)
                }
            }

            for ((loc, recordedWork) in filteredWork) {
                val existing = prepareDrawerCmdBuffers.getOrPut(loc.first) { mutableMapOf() }.put(loc.second, recordedWork.cmdBuffer)
                if (existing != null)
                    throw Exception("oh no this should really not happen!")
            }
        }

        var passIndex = 0
        val globalStates = mutableMapOf<VulkanRenderBuffer, UsageType>()
        for (graphNode in sequencedGraph) {
            when (graphNode) {
                is VulkanPassInstance -> {
                    val pass = graphNode.pass
                    pass.render(frame, graphNode, globalStates, prepareDrawerCmdBuffers[graphNode] ?: emptyMap())
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

        frame.frameDataAllocator.beforeSubmission()

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
            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, VK_NULL_HANDLE).ensureIs("Failed to submit command buffers", VK_SUCCESS)
            backend.logicalDevice.graphicsQueue.mutex.release()
        }

        blitHelper.copyFinalRenderbuffer(frame, passesInstances.last().resolvedOutputs[vulkanPasses.last().declaration.outputs.outputs[0]]!!)
    }

    fun resizeBuffers() {
        tasks.values.forEach { it ->
            it.buffers.values.forEach { it.resize() }
            it.passes.values.forEach { it.dumpFramebuffers() }
        }
    }

    override fun cleanup() {
        dispatchingSystems.forEach(Cleanable::cleanup)
        tasks.values.forEach(Cleanable::cleanup)
        blitHelper.cleanup()

        commandPool.cleanup()

        for(cleanupHook in declaration.cleanupHooks) {
            cleanupHook()
        }
    }
}