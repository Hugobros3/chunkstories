package io.xol.chunkstories.graphics.vulkan.systems.world

import io.xol.chunkstories.api.Location
import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.graphics.Camera
import io.xol.chunkstories.api.util.kotlin.toVec3i
import io.xol.chunkstories.api.voxel.Voxel
import io.xol.chunkstories.api.voxel.VoxelFormat
import io.xol.chunkstories.api.voxel.VoxelSide
import io.xol.chunkstories.graphics.common.Primitive
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import io.xol.chunkstories.world.WorldImplementation
import org.joml.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import kotlin.random.Random

class VulkanCubesDrawer(pass: VulkanPass, val client: IngameClient) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    //val guiShaderProgram = backend.shaderFactory.createProgram(backend, "/shaders/cubes/cubes")
    val descriptorPool = DescriptorPool(backend, pass.program)
    val vertexInputConfiguration = vertexInputConfiguration {
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

    val pipeline = Pipeline(backend, pass, vertexInputConfiguration, Primitive.TRIANGLES)

    private val vertexBuffer: VulkanVertexBuffer
    val individualCubeVertices = floatArrayOf(
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

    var instances = 0
    var lastGenPosition = Vector3d(-100.0)
    fun fillInstanceBuffer(frame:Frame, arround: Location) {
        println("waiting on previous frame to finish so we know we can update the buffer")
        var previousIndex = frame.inflightFrameIndex - 1
        if(previousIndex < 0)
            previousIndex += backend.swapchain.maxFramesInFlight
        if(previousIndex != frame.inflightFrameIndex) {
            //TODO this is terrible
            vkDeviceWaitIdle(backend.logicalDevice.vkDevice)
        }
        println("filling instance buffer")

        val buffer = MemoryUtil.memAlloc(instancesBuffer.bufferSize.toInt())

        val arroundi = arround.toVec3i()
        val radius = 128
        val radiush = 32

        val rng = Random(arround.hashCode())
        val world = client.world as WorldImplementation

        instances = 0
        for(x in arroundi.x - radius until arroundi.x + radius) {
            for(y in arroundi.y - radiush until arroundi.y + radiush) {
                for(z in arroundi.z - radius until arroundi.z + radius) {
                    //val cell = world.peekSafely(x, y, z)
                    val data = world.peekRaw(x, y, z)
                    val voxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(data))!!

                    fun opaque(voxel: Voxel) = voxel.solid || voxel.name == "water"

                    fun check(x2: Int, y2: Int, z2: Int) : Boolean {
                        val data2 = world.peekRaw(x2, y2, z2)
                        val voxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(data2))!!
                        return opaque(voxel)
                    }

                    //TODO cell.voxel never null
                    if(opaque(voxel)) {
                        if(check(x, y - 1, z) && check(x, y + 1, z) && check(x + 1, y, z) && check(x - 1, y, z) && check(x, y, z + 1) && check(x, y, z - 1))
                            continue

                        buffer.putFloat(x.toFloat())
                        buffer.putFloat(y.toFloat())
                        buffer.putFloat(z.toFloat())

                        val tex = voxel.voxelTextures[VoxelSide.TOP.ordinal]//voxel?.getVoxelTexture(cell, VoxelSide.TOP)
                        val color = Vector4f(tex.color ?: Vector4f(1f, 0f, 0f, 1f))
                        if(color.w < 1.0f)
                            color.mul(Vector4f(0f, 1f, 0.3f, 1.0f))
                        //color.mul(cell.sunlight / 15f)
                        color.mul(0.9f + rng.nextFloat() * 0.1f)

                        //val color = Vector4f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat(), 1f)
                        buffer.putFloat(color.x())
                        buffer.putFloat(color.y())
                        buffer.putFloat(color.z())
                        instances++
                    }
                }
            }
        }
        buffer.flip()
        println("instances $instances $maxCubeInstances")

        instancesBuffer.upload(buffer)
        lastGenPosition = Vector3d(arround)
        MemoryUtil.memFree(buffer)
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        val entity = client.player.controlledEntity
        if(entity != null) {
            val camera = entity.traits[TraitControllable::class]?.camera ?: Camera()
            descriptorPool.configure(frame, camera)

            //println("$lastGenPosition ${lastGenPosition.distance(entity.location)}")

            if(lastGenPosition.distance(entity.location) > 32) {
                fillInstanceBuffer(frame, entity.location)
            }
        }

        if(instances > 0) {
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 1, descriptorPool.setsForFrame(frame), null as? IntArray)

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
            vkCmdBindVertexBuffers(commandBuffer, 1, stackLongs(instancesBuffer.handle), stackLongs(0))

            vkCmdDraw(commandBuffer, 3 * 2 * 6, instances, 0, 0)
        }
    }

    override fun cleanup() {
        vertexBuffer.cleanup()
        instancesBuffer.cleanup()

        descriptorPool.cleanup()

        pipeline.cleanup()
    }
}
