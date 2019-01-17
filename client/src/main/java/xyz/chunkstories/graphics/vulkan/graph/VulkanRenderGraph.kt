package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.dsl.*
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import org.joml.Vector2i
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.ImageInput
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.graphics.vulkan.systems.VulkanFullscreenQuadDrawer
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, val script: RenderGraphDeclarationScript) : RenderGraph, Cleanable {

    val commandPool: CommandPool
    val commandBuffers: InflightFrameResource<VkCommandBuffer>

    override val buffers = mutableMapOf<String, VulkanRenderBuffer>()
    override val passes = mutableMapOf<String, VulkanPass>()

    override lateinit var defaultPass: VulkanPass
    override lateinit var finalPass: VulkanPass

    override val viewportSize: Vector2i
            get() = Vector2i(backend.window.width, backend.window.height)

    var passesInOrder: List<VulkanPass>

    private val parser = VulkanRenderGraphBuilder(this)

    internal val dummySwapchainRenderBuffer: VulkanRenderBuffer

    init {
        commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        parser.apply(script)

        defaultPass = passes.values.find { it.default } ?: throw Exception("No default pass was set !")
        finalPass = passes.values.find { it.final } ?: throw Exception("No final pass was set !")

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

        passesInOrder = buildGraph()

        dummySwapchainRenderBuffer = VulkanRenderBuffer(backend, this) {
            name = "_swapchain"
        }

        val presentPass = PresentationPass(backend, this) {
            this.shaderName = "blit"

            this.dependencies.add(passesInOrder.last().name)

            this.imageInputs.add(ImageInput().apply {
                this.name = "diffuseTexture"
                this.source = ImageInput.ImageSource.RenderBufferReference(passesInOrder.last().outputs[0].name)
            })

            this.outputs.add(PassOutput().apply {
                this.name = "_swapchain"

                this.blending = PassOutput.BlendMode.OVERWRITE
            })

            declaredDrawingSystems.add(RegisteredDrawingSystem(FullscreenQuadDrawer::class.java) {
                /*val fsDrawer = this as VulkanFullscreenQuadDrawer
                fsDrawer.shaderBindings {
                    it.bindTextureAndSampler("diffuseTexture", passesInOrder.last().outputRenderBuffers[0].texture, VulkanSampler(backend))
                }*/
            })
        }

        passesInOrder = passesInOrder.toMutableList()
        (passesInOrder as MutableList<VulkanPass>).add(presentPass)

        passesInOrder.forEach { it.postGraphBuild() }
    }

    private fun buildGraph() : List<VulkanPass> {
        // First resolve the named dependencies into actual VulkanPass objects
        passes.values.forEach { pass -> pass.passDependencies =  pass.dependencies.map { passes[it] ?: throw Exception("Missing pass $it") } }

        // Check there are no loops !
        fun hasLoop(pass: VulkanPass, banned: Set<VulkanPass>) : Boolean {
            // If the intersection between the dependencies and the banned set isn't empty, then we have a loop
            if(banned.intersect(pass.passDependencies).isNotEmpty())
                return true

            // We add the current pass to the set of banned ones
            val newBanned = banned.union(setOf(pass))

            //Any match ? We're done
            pass.passDependencies.forEach {
                if(hasLoop(it, newBanned))
                    return true
            }

            return false
        }

        if(hasLoop(finalPass, emptySet()))
            throw Exception("Render graph has loops ! Bad :(")

        val inOrder = mutableListOf<VulkanPass>()

        fun insertWithDependencies(pass: VulkanPass) {
            pass.passDependencies.forEach { insertWithDependencies(it) }

            if(!inOrder.contains(pass))
                inOrder.add(pass)
        }

        insertWithDependencies(finalPass)

        return inOrder
    }

    fun renderFrame(frame: Frame) {

        for(pass in passesInOrder) {
            pass.render(frame, null)
        }

        stackPush().use {
            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val commandBuffers = MemoryStack.stackMallocPointer(passesInOrder.size)
                for (pass in passesInOrder)
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

                val semaphoresToSignal = MemoryStack.stackLongs(frame.renderFinishedSemaphore)
                pSignalSemaphores(semaphoresToSignal)
            }

            //println("submitting: ${submitInfo.waitSemaphoreCount()} + ${submitInfo.signalSemaphoreCount()}")

            backend.logicalDevice.graphicsQueue.mutex.acquireUninterruptibly()
            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, frame.renderFinishedFence).ensureIs("Failed to submit command buffer", VK_SUCCESS)
            backend.logicalDevice.graphicsQueue.mutex.release()

            //println("frame ${frame.frameNumber} ifi: ${frame.inflightFrameIndex} semIn: ${frame.renderCanBeginSemaphore} semOut: ${frame.renderFinishedSemaphore}")

            //   stackPop()
        }

        //copyFinalRenderbuffer(frame)
    }

    private fun copyFinalRenderbuffer(frame: Frame)  {
        stackPush().use {
            commandBuffers[frame].apply {
                val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                    flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    pInheritanceInfo(null)
                }

                vkBeginCommandBuffer(this, beginInfo)

                val barriers = VkImageMemoryBarrier.callocStack(2)

                val finalRenderBufferReadyCopyBarrier = barriers[1].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                    //TODO check last use
                    oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                    srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                    image(finalPass.outputRenderBuffers[0].texture.imageHandle)

                    subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_COLOR_ATTACHMENT_READ_BIT)
                    dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                }
                val swapchainImageReadyCopyBarrier = barriers[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                    oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                    image(frame.swapchainImage)

                    subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    srcAccessMask(0)
                    dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                }
                vkCmdPipelineBarrier(this, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barriers)

                val region = VkImageCopy.callocStack(1).apply {
                    this.extent().width(finalPass.outputRenderBuffers[0].size.x)
                    this.extent().height(finalPass.outputRenderBuffers[0].size.y)
                    this.extent().depth(1)

                    srcSubresource().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        mipLevel(0)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    dstSubresource().set(srcSubresource())
                }

                vkCmdCopyImage(this, finalPass.outputRenderBuffers[0].texture.imageHandle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                       frame.swapchainImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

                val swapchainImageReadyPresentBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                    oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                    srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    image(frame.swapchainImage)

                    subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT)
                }

                vkCmdPipelineBarrier(this, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, null, null, swapchainImageReadyPresentBarrier)

                //vkCmdEndRenderPass(this)
                vkEndCommandBuffer(this)
            }

            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val commandBuffers = MemoryStack.stackMallocPointer(1)
                commandBuffers.put(0, this@VulkanRenderGraph.commandBuffers[frame])
                pCommandBuffers(commandBuffers)

                val waitOnSemaphores = MemoryStack.stackMallocLong(1)
                waitOnSemaphores.put(0, frame.renderCanBeginSemaphore)
                pWaitSemaphores(waitOnSemaphores)
                waitSemaphoreCount(1)

                val waitStages = MemoryStack.stackMallocInt(1)
                waitStages.put(0, VK_PIPELINE_STAGE_TRANSFER_BIT)
                pWaitDstStageMask(waitStages)

                val semaphoresToSignal = MemoryStack.stackLongs(frame.renderFinishedSemaphore)
                pSignalSemaphores(semaphoresToSignal)
            }

            backend.logicalDevice.graphicsQueue.mutex.acquireUninterruptibly()
            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, frame.renderFinishedFence).ensureIs("Failed to submit command buffer", VK_SUCCESS)
            backend.logicalDevice.graphicsQueue.mutex.release()
        }
    }

    override fun cleanup() {
        passes.values.forEach(Cleanable::cleanup)
        buffers.values.forEach(Cleanable::cleanup)

        commandPool.cleanup()
        //commandBuffers.cleanup() // useless, cleaning the commandpool cleans those implicitely
    }

    fun resizeBuffers() {
        buffers.values.forEach { it.resize() }
        passes.values.forEach { it.recreateFramebuffer() }
    }
}