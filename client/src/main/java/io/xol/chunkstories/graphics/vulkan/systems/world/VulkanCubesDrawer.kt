package io.xol.chunkstories.graphics.vulkan.systems.world

import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.graphics.structs.Camera
import io.xol.chunkstories.api.physics.Box
import io.xol.chunkstories.api.physics.Frustrum
import io.xol.chunkstories.api.util.kotlin.toVec3d
import io.xol.chunkstories.api.util.kotlin.toVec3i
import io.xol.chunkstories.client.InternalClientOptions
import io.xol.chunkstories.graphics.common.Primitive
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import io.xol.chunkstories.world.WorldClientCommon
import io.xol.chunkstories.world.WorldImplementation
import io.xol.chunkstories.world.chunk.CubicChunk
import org.joml.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class VulkanCubesDrawer(pass: VulkanPass, val client: IngameClient) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    private val descriptorPool = DescriptorPool(backend, pass.program)
    private val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding(0)
            stride(3 * 4 + 2 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }
        binding {
            binding(1)
            stride(3 * 4 + 3 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_INSTANCE)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }!!.location)
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(0)
        }
        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "texCoordIn" }!!.location)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(3 * 4)
        }

        attribute {
            binding(1)
            location(program.vertexInputs.find { it.name == "cubePositionIn" }!!.location)
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(0)
        }
        attribute {
            binding(1)
            location(program.vertexInputs.find { it.name == "cubeColorIn" }!!.location)
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(3 * 4)
        }
    }

    private val pipeline = Pipeline(backend, pass, vertexInputConfiguration, Primitive.TRIANGLES)

    private val vertexBuffer: VulkanVertexBuffer
    private val individualCubeVertices = floatArrayOf(
             0.0f,  0.0f,  0.0f,   0.0f, 0.0f,
             0.0f,  1.0f,  1.0f,   1.0f, 1.0f,
             0.0f,  1.0f,  0.0f,   0.0f, 1.0f,
             0.0f,  1.0f,  1.0f,   1.0f, 1.0f,
             0.0f,  0.0f,  0.0f,   0.0f, 0.0f,
             0.0f,  0.0f,  1.0f,   1.0f, 0.0f,

             0.0f,  0.0f,  1.0f,   0.0f, 0.0f,
            1.0f,   0.0f,  1.0f,   1.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 1.0f,
             0.0f,  0.0f,  1.0f,   0.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 1.0f,
             0.0f,  1.0f,  1.0f,   0.0f, 1.0f,

            1.0f,   0.0f,  0.0f,   1.0f, 0.0f,
            1.0f,   1.0f,  0.0f,   1.0f, 1.0f,
            1.0f,   1.0f,  1.0f,   0.0f, 1.0f,
            1.0f,   0.0f,  0.0f,   1.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   0.0f, 1.0f,
            1.0f,   0.0f,  1.0f,   0.0f, 0.0f,

             0.0f,  0.0f,  0.0f,   1.0f, 0.0f,
            1.0f,   1.0f,  0.0f,   0.0f, 1.0f,
            1.0f,   0.0f,  0.0f,   0.0f, 0.0f,
             0.0f,  0.0f,  0.0f,   1.0f, 0.0f,
             0.0f,  1.0f,  0.0f,   1.0f, 1.0f,
            1.0f,   1.0f,  0.0f,   0.0f, 1.0f,

             0.0f,  1.0f,  0.0f,   0.0f, 1.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 0.0f,
            1.0f,   1.0f,  0.0f,   1.0f, 1.0f,
             0.0f,  1.0f,  0.0f,   0.0f, 1.0f,
             0.0f,  1.0f,  1.0f,   0.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 0.0f,

             0.0f,  0.0f,  0.0f,   0.0f, 0.0f,
            1.0f,   0.0f,  0.0f,   1.0f, 0.0f,
            1.0f,   0.0f,  1.0f,   1.0f, 1.0f,
             0.0f,  0.0f,  0.0f,   0.0f, 0.0f,
            1.0f,   0.0f,  1.0f,   1.0f, 1.0f,
             0.0f,  0.0f,  1.0f,   0.0f, 1.0f
    )

    private val instancesBuffer: VulkanVertexBuffer
    private val maxCubeInstances = 1024 * 1024 // max 1M cubes ?

    init {
        vertexBuffer = VulkanVertexBuffer(backend, individualCubeVertices.size * 4L)
        instancesBuffer = VulkanVertexBuffer(backend, maxCubeInstances * 3 * 4L * 2L)

        stackPush().use {
            val byteBuffer = stackMalloc(individualCubeVertices.size * 4)
            individualCubeVertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    companion object {
        var totalCubesDrawn = 0
        var totalBuffersUsed = 0
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        val entity = client.player.controlledEntity
        val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        descriptorPool.configure(frame, camera)

        val frustrum = Frustrum(camera, client.gameWindow)

        totalCubesDrawn = 0
        totalBuffersUsed = 0

        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 1, descriptorPool.setsForFrame(frame), null as? IntArray)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))

        /*if(instances > 0) {
            vkCmdBindVertexBuffers(commandBuffer, 1, stackLongs(instancesBuffer.handle), stackLongs(0))
            vkCmdDraw(commandBuffer, 3 * 2 * 6, instances, 0, 0)
        }*/

        val world = client.world as WorldClientCommon

        val camChunk = camera.position.toVec3i()
        camChunk.x /= 32
        camChunk.y /= 32
        camChunk.z /= 32

        val drawDistance = world.client.configuration.getIntValue(InternalClientOptions.viewDistance) / 32
        val drawDistanceH = 4

        val usedData = mutableListOf<VulkanChunkRenderData.Block>()
        for(chunk in world.allLoadedChunks) {
            val chunk = chunk as CubicChunk

            if(!frustrum.isBoxInFrustrum(Box(Vector3i(chunk.chunkX * 32, chunk.chunkY * 32, chunk.chunkZ * 32).toVec3d(), Vector3i(32, 32, 32).toVec3d())))
                continue

            if(chunk.isAirChunk)
                continue

            if(chunk.chunkX !in (camChunk.x - drawDistance)..(camChunk.x + drawDistance))
                continue

            if((chunk.chunkZ !in (camChunk.z - drawDistance)..(camChunk.z + drawDistance)))
                continue

            if((chunk.chunkY !in (camChunk.y - drawDistanceH)..(camChunk.y + drawDistanceH)))
                continue

            if(chunk.meshData is VulkanChunkRenderData) {

                val block = (chunk.meshData as VulkanChunkRenderData).getLastBlock()
                if(block?.vertexBuffer != null) {
                    vkCmdBindVertexBuffers(commandBuffer, 1, stackLongs(block.vertexBuffer.handle), stackLongs(0))
                    vkCmdDraw(commandBuffer, 3 * 2 * 6, block.count, 0, 0)

                    totalCubesDrawn += block.count
                    totalBuffersUsed++
                }
            } else {
                chunk.meshData = VulkanChunkRenderData(backend, chunk)
            }
        }


        frame.recyclingTasks.add {
            usedData.forEach(VulkanChunkRenderData.Block::doneWith)
        }
    }

    override fun cleanup() {
        for(chunk in client.world.allLoadedChunks) {
            (chunk.mesh() as? VulkanChunkRenderData)?.destroy()
        }

        vertexBuffer.cleanup()
        instancesBuffer.cleanup()

        descriptorPool.cleanup()

        pipeline.cleanup()
    }
}
