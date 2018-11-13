package io.xol.chunkstories.graphics.vulkan.systems.debug

import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.graphics.structs.Camera
import io.xol.chunkstories.client.InternalClientOptions
import io.xol.chunkstories.graphics.common.FaceCullingMode
import io.xol.chunkstories.graphics.common.Primitive
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.resources.InflightFrameResource
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import io.xol.chunkstories.graphics.vulkan.systems.gui.guiBufferSize
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import io.xol.chunkstories.world.WorldImplementation
import org.joml.Vector3d
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class VulkanDebugDrawer(pass: VulkanPass, val client: IngameClient) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    //val guiShaderProgram = backend.shaderFactory.createProgram(backend, "/shaders/cubes/cubes")
    val descriptorPool = DescriptorPool(backend, pass.program)
    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding(0)
            stride(3 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }!!.location)
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(0)
        }
    }

    val pipeline = Pipeline(backend, pass, vertexInputConfiguration, Primitive.LINES, FaceCullingMode.DISABLED)

    val vertexBuffers: InflightFrameResource<VulkanVertexBuffer>

    val debugBufferSize = 1024 * 1024 * 2

    init {
        vertexBuffers = InflightFrameResource(backend) {
            VulkanVertexBuffer(backend, debugBufferSize.toLong())
        }
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        val entity = client.player.controlledEntity
        val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        descriptorPool.configure(frame, camera)

        var linesCount = 0
        val buffer = memAlloc(debugBufferSize)

        fun line(from: Vector3d, to: Vector3d) {
            buffer.putFloat(from.x().toFloat())
            buffer.putFloat(from.y().toFloat())
            buffer.putFloat(from.z().toFloat())

            buffer.putFloat(to.x().toFloat())
            buffer.putFloat(to.y().toFloat())
            buffer.putFloat(to.z().toFloat())
            linesCount++
        }

        fun cube(from: Vector3d, to: Vector3d) {
            val p000 = from
            val p001 = Vector3d(from.x, from.y, to.z)
            val p010 = Vector3d(from.x, to.y, from.z)
            val p011 = Vector3d(from.x, to.y, to.z)
            val p100 = Vector3d(to.x, from.y, from.z)
            val p101 = Vector3d(to.x, from.y, to.z)
            val p110 = Vector3d(to.x, to.y, from.z)
            val p111 = to

            //00y
            line(p000, p001)
            line(p010, p011)
            line(p100, p101)
            line(p110, p111)

            //x00
            line(p000, p100)
            line(p001, p101)
            line(p010, p110)
            line(p011, p111)

            //0y0
            line(p000, p010)
            line(p001, p011)
            line(p100, p110)
            line(p101, p111)
        }

        if(client.configuration.getBooleanValue(InternalClientOptions.debugWireframe)) {
            val world = client.world as WorldImplementation
            for (chunk in world.allLoadedChunks) {
                val cp = Vector3d((chunk.chunkX * 32).toDouble(), (chunk.chunkY * 32).toDouble(), (chunk.chunkZ * 32).toDouble())
                val cpe = Vector3d(cp).add(32.0, 32.0, 32.0)

                if(buffer.remaining() <= 4 * 3 * 2 * 4 * 3 * 5)
                    break
                cube(cp, cpe)
            }
        }

        buffer.flip()
        vertexBuffers[frame].upload(buffer)

        memFree(buffer)

        if(linesCount > 0) {
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 1, descriptorPool.setsForFrame(frame), null as? IntArray)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffers[frame].handle), stackLongs(0))
            vkCmdDraw(commandBuffer, 2 * linesCount, 1, 0, 0)
        }
    }

    override fun cleanup() {
        vertexBuffers.cleanup()

        descriptorPool.cleanup()

        pipeline.cleanup()
    }
}
