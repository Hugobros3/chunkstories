package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.graphics.vulkan.CommandPool
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.shaders.UniformTestOffset
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.resources.PerFrameResource
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import io.xol.chunkstories.gui.ClientGui
import org.joml.Vector4f
import org.joml.Vector4fc
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

internal const val guiBufferSize = 16384

class VulkanGuiPass(val backend: VulkanGraphicsBackend, val gui: ClientGui) {
    val baseProgram = backend.shaderFactory.createProgram(backend, "/shaders/blit")

    val pipeline = Pipeline(backend, backend.renderToBackbuffer, baseProgram)
    val cmdPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
    val descriptorPool = DescriptorPool(backend, baseProgram)

    val vertexBuffers: PerFrameResource<VulkanVertexBuffer>
    val commandBuffers: PerFrameResource<VkCommandBuffer>

    val stagingByteBuffer = MemoryUtil.memAlloc(guiBufferSize)
    val stagingFloatBuffer = stagingByteBuffer.asFloatBuffer()
    var stagingSize = 0

    val drawer = object : DummyGuiDrawer(gui) {
        override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: String?, color: Vector4fc?) {
            val sx = 1.0F / gui.viewportWidth.toFloat()
            val sy = 1.0F / gui.viewportHeight.toFloat()

            fun vertex(a: Int, b: Int) {
                stagingFloatBuffer.put(-1.0F + 2.0F * (a * sx))
                stagingFloatBuffer.put(1.0F - 2.0F * (b * sy))
            }
            
            vertex((startX), startY)
            vertex((startX), (startY + height))
            vertex((startX + width), (startY + height))

            vertex((startX), startY)
            vertex((startX + width), (startY))
            vertex((startX + width), (startY + height))

            stagingSize += 2
        }


        override fun drawBoxWithCorners(posx: Int, posy: Int, width: Int, height: Int, cornerSizeDivider: Int, texture: String) {
            drawBox(posx, posy, width, height, Vector4f(1.0F))
        }

        override fun drawString(font: Font, xPosition: Int, yPosition: Int, text: String, color: Vector4fc) {
            println(text)
        }
    }

    init {
        stackPush()

        vertexBuffers = PerFrameResource(backend) {
            VulkanVertexBuffer(backend, guiBufferSize.toLong())
        }

        commandBuffers = PerFrameResource(backend) {
            val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
                commandPool(cmdPool.handle)
                level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                commandBufferCount(1)
            }

            val pCmdBuffers = stackMallocPointer(1)
            vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, commandBufferAllocateInfo, pCmdBuffers)

            val commandBuffer = VkCommandBuffer(pCmdBuffers.get(0), backend.logicalDevice.vkDevice)

            commandBuffer
        }

        stackPop()
    }

    fun render(frame : Frame) {
        stackPush().use {

            stagingByteBuffer.clear()
            stagingFloatBuffer.clear()
            stagingSize = 0

            gui.topLayer?.render(drawer)

            // Rewrite the vertex buffer
            vertexBuffers[frame].apply {
                this.upload(stagingByteBuffer)
            }

            val testOffset = UniformTestOffset()
            testOffset.offset.x = (Math.random().toFloat() - 0.5F) * 0.2F

            descriptorPool.configure(frame, testOffset)

            // Rewrite the command buffer
            commandBuffers[frame].apply {
                val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                    flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    pInheritanceInfo(null)
                }

                vkBeginCommandBuffer(this, beginInfo)

                val viewport = VkViewport.callocStack(1).apply {
                    x(0.0F)
                    y(0.0F)
                    width(backend.window.width.toFloat())
                    height(backend.window.height.toFloat())
                    minDepth(0.0F)
                    maxDepth(1.0F)
                }

                val zeroZero = VkOffset2D.callocStack().apply {
                    x(0)
                    y(0)
                }
                val scissor = VkRect2D.callocStack(1).apply {
                    offset(zeroZero)
                    extent().width(backend.window.width)
                    extent().height(backend.window.height)
                }

                vkCmdSetViewport(this, 0, viewport)
                vkCmdSetScissor(this, 0, scissor)

                vkCmdBindDescriptorSets(this, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 0, descriptorPool.setsForFrame(frame), null as? IntArray)

                val renderPassBeginInfo = VkRenderPassBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).apply {
                    renderPass(backend.renderToBackbuffer.handle)

                    framebuffer(frame.swapchainFramebuffer)
                    renderArea().offset().x(0)
                    renderArea().offset().y(0)
                    renderArea().extent().width(backend.window.width)
                    renderArea().extent().height(backend.window.height)

                    val clearColor = VkClearValue.callocStack(1).apply {
                        color().float32().apply {
                            this.put(0, 0.0F)
                            this.put(1, 0.5F)
                            this.put(2, 0.0F)
                            this.put(3, 1.0F)
                        }
                    }
                    pClearValues(clearColor)
                }

                vkCmdBeginRenderPass(this, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)
                vkCmdBindPipeline(this, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

                vkCmdBindVertexBuffers(this, 0, stackLongs(vertexBuffers[frame].handle), stackLongs(0))

                vkCmdDraw(this, 3 * stagingSize, 1, 0, 0) // that's rather anticlimactic
                vkCmdEndRenderPass(this)

                vkEndCommandBuffer(this)
            }

            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val waitOnSemaphores = stackMallocLong(1)
                waitOnSemaphores.put(0, frame.renderCanBeginSemaphore)
                pWaitSemaphores(waitOnSemaphores)
                waitSemaphoreCount(1)

                val waitStages = stackMallocInt(1)
                waitStages.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                pWaitDstStageMask(waitStages)

                val commandBuffers = stackMallocPointer(1)
                commandBuffers.put(0, this@VulkanGuiPass.commandBuffers[frame])
                pCommandBuffers(commandBuffers)

                val semaphoresToSignal = stackLongs(frame.renderFinishedSemaphore)
                pSignalSemaphores(semaphoresToSignal)
            }

            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, frame.renderFinishedFence).ensureIs("Failed to submit command buffer", VK_SUCCESS)
        }
    }

    fun cleanup() {
        vertexBuffers.cleanup()

        cmdPool.cleanup()
        pipeline.cleanup()

        baseProgram.cleanup()

        descriptorPool.cleanup()

        MemoryUtil.memFree(stagingByteBuffer)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan.triangleTest")
    }

}