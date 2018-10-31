package io.xol.chunkstories.graphics.vulkan.graph

import io.xol.chunkstories.api.dsl.*
import io.xol.chunkstories.api.graphics.rendergraph.*
import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.graphics.vulkan.CommandPool
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.resources.PerFrameResource
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.joml.Vector2i
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*
import kotlin.reflect.KClass

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, script: RenderGraphDeclarationScript) : RenderGraph, Cleanable {

    val commandPool: CommandPool
    val commandBuffers: PerFrameResource<VkCommandBuffer>

    override val buffers = mutableMapOf<String, VulkanRenderBuffer>()
    override val passes = mutableMapOf<String, VulkanPass>()

    override lateinit var defaultPass: VulkanPass
    override lateinit var finalPass: VulkanPass

    override val viewportSize: Vector2i
            get() = Vector2i(backend.window.width, backend.window.height)

    val parser = object : RenderGraphDeclarationsContext {
        override val renderGraph = this@VulkanRenderGraph

        /** Enter the context to declare a bunch of RenderBuffers */
        override fun renderBuffers(renderBufferDeclarations: RenderBuffersDeclarationCtx.() -> Unit) = object : RenderBuffersDeclarationCtx {

            /** Declare a render buffer and add it to the graph */
            override fun renderBuffer(renderBufferConfiguration: RenderBuffer.() -> Unit) {
                val renderBuffer = VulkanRenderBuffer(backend, this@VulkanRenderGraph, renderBufferConfiguration)
                buffers[renderBuffer.name] = renderBuffer
            }
        }.apply(renderBufferDeclarations)

        /** Enter the context to declare a bunch of Passes */
        override fun passes(function: PassesDeclarationCtx.() -> Unit) = object : PassesDeclarationCtx {
            /** Declare a pass and add it to the graph */
            override fun pass(config: Pass.() -> Unit) {
                val pass = VulkanPass(backend, this@VulkanRenderGraph, config)
                passes[pass.name] = pass
            }

            override fun Pass.outputs(outputsDeclarations: PassOutputsDeclarationCtx.() -> Unit): PassOutputsDeclarationCtx {
                return object : PassOutputsDeclarationCtx {
                    override fun output(outputConfiguration: PassOutput.() -> Unit) {
                        val output = PassOutput().apply(outputConfiguration)
                        this@outputs.outputs.add(output)
                    }

                }.apply(outputsDeclarations)
            }

            override fun Pass.draws(drawsDeclarations: DrawsDeclarationCtx.() -> Unit) { object : DrawsDeclarationCtx {
                override fun <T : DrawingSystem> system(systemClass: KClass<T>, systemConfiguration: T.() -> Unit) {
                    this@draws.declaredDrawingSystems.add(RegisteredDrawingSystem(systemClass.java, systemConfiguration as DrawingSystem.() -> Unit))
                }

            }.apply(drawsDeclarations)}

        }.apply(function)
    }

    init {
        commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        parser.apply(script)

        defaultPass = passes.values.find { it.default } ?: throw Exception("No default pass was set !")
        finalPass = passes.values.find { it.final } ?: throw Exception("No final pass was set !")

        commandBuffers = PerFrameResource(backend) {
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
    }

    fun renderFrame(frame: Frame) {
        //TODO build some layout and have inter-pass synchronisation
        finalPass.render(frame, frame.renderCanBeginSemaphore)

        copyFinalRenderbuffer(frame)
    }

    private fun copyFinalRenderbuffer(frame: Frame) {
        stackPush().use {
            commandBuffers[frame].apply {
                val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                    flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    pInheritanceInfo(null)
                }

                vkBeginCommandBuffer(this, beginInfo)

                val imageBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
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

                vkCmdPipelineBarrier(this, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, imageBarrier)

                val region = VkImageCopy.callocStack(1).apply {
                    this.extent().width(finalPass.renderBuffers[0].size.x)
                    this.extent().height(finalPass.renderBuffers[0].size.y)
                    this.extent().depth(1)

                    srcSubresource().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        mipLevel(0)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    dstSubresource().set(srcSubresource())
                }
                vkCmdCopyImage(this, finalPass.renderBuffers[0].texture.imageHandle, VK_IMAGE_LAYOUT_GENERAL,
                        frame.swapchainImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

                val imageBarrier2 = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
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

                vkCmdPipelineBarrier(this, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, null, null, imageBarrier2)

                //vkCmdEndRenderPass(this)
                vkEndCommandBuffer(this)
            }

            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val waitOnSemaphores = MemoryStack.stackMallocLong(1)
                waitOnSemaphores.put(0, finalPass.passDoneSemaphore)
                pWaitSemaphores(waitOnSemaphores)
                waitSemaphoreCount(1)

                val waitStages = MemoryStack.stackMallocInt(1)
                waitStages.put(0, VK_PIPELINE_STAGE_TRANSFER_BIT)
                pWaitDstStageMask(waitStages)

                val commandBuffers = MemoryStack.stackMallocPointer(1)
                commandBuffers.put(0, this@VulkanRenderGraph.commandBuffers[frame])
                pCommandBuffers(commandBuffers)

                val semaphoresToSignal = MemoryStack.stackLongs(frame.renderFinishedSemaphore)
                pSignalSemaphores(semaphoresToSignal)
            }

            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, frame.renderFinishedFence).ensureIs("Failed to submit command buffer", VK_SUCCESS)
        }
    }

    override fun cleanup() {
        passes.values.forEach(Cleanable::cleanup)
        buffers.values.forEach(Cleanable::cleanup)

        commandPool.cleanup()
        //commandBuffers.cleanup() // useless, cleaning the commandpool cleans those implicitely
    }

    fun resizeBuffers() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}