package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.Location
import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.entity.Entity
import io.xol.chunkstories.api.entity.traits.serializable.TraitRotation
import io.xol.chunkstories.api.graphics.Camera
import io.xol.chunkstories.api.world.World
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import io.xol.chunkstories.util.math.toVec3f
import io.xol.chunkstories.util.math.toVec3i
import org.joml.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

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
            stride(3 * 4)
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
    }

    val pipeline = Pipeline(backend, pass, vertexInputConfiguration)

    private val vertexBuffer: VulkanVertexBuffer
    val individualCubeVertices = floatArrayOf(
            -1.0f, -1.0f, -1.0f,   0.0f, 0.0f,
            -1.0f,  1.0f,  1.0f,   1.0f, 1.0f,
            -1.0f,  1.0f, -1.0f,   0.0f, 1.0f,
            -1.0f,  1.0f,  1.0f,   1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,   0.0f, 0.0f,
            -1.0f, -1.0f,  1.0f,   1.0f, 0.0f,

            -1.0f, -1.0f,  1.0f,   0.0f, 0.0f,
            1.0f,  -1.0f,  1.0f,   1.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 1.0f,
            -1.0f, -1.0f,  1.0f,   0.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 1.0f,
            -1.0f,  1.0f,  1.0f,   0.0f, 1.0f,

            1.0f,  -1.0f, -1.0f,   1.0f, 0.0f,
            1.0f,   1.0f, -1.0f,   1.0f, 1.0f,
            1.0f,   1.0f,  1.0f,   0.0f, 1.0f,
            1.0f,  -1.0f, -1.0f,   1.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   0.0f, 1.0f,
            1.0f,  -1.0f,  1.0f,   0.0f, 0.0f,

            -1.0f, -1.0f, -1.0f,   1.0f, 0.0f,
            1.0f,   1.0f, -1.0f,   0.0f, 1.0f,
            1.0f,  -1.0f, -1.0f,   0.0f, 0.0f,
            -1.0f, -1.0f, -1.0f,   1.0f, 0.0f,
            -1.0f,  1.0f, -1.0f,   1.0f, 1.0f,
            1.0f,   1.0f, -1.0f,   0.0f, 1.0f,

            -1.0f,  1.0f, -1.0f,   0.0f, 1.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 0.0f,
            1.0f,   1.0f, -1.0f,   1.0f, 1.0f,
            -1.0f,  1.0f, -1.0f,   0.0f, 1.0f,
            -1.0f,  1.0f,  1.0f,   0.0f, 0.0f,
            1.0f,   1.0f,  1.0f,   1.0f, 0.0f,

            -1.0f, -1.0f, -1.0f,   0.0f, 0.0f,
            1.0f,  -1.0f, -1.0f,   1.0f, 0.0f,
            1.0f,  -1.0f,  1.0f,   1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,   0.0f, 0.0f,
            1.0f,  -1.0f,  1.0f,   1.0f, 1.0f,
            -1.0f, -1.0f,  1.0f,   0.0f, 1.0f
    )

    private val instancesBuffer: VulkanVertexBuffer
    private val maxCubeInstances = 1024 * 1024 // max 1M cubes ?

    init {
        vertexBuffer = VulkanVertexBuffer(backend, individualCubeVertices.size * 4L)
        instancesBuffer = VulkanVertexBuffer(backend, maxCubeInstances * 3 * 4L)

        stackPush().use {
            val byteBuffer = stackMalloc(individualCubeVertices.size * 4)
            individualCubeVertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    var instances = 0
    var lastGenPosition = Vector3d(-100.0)
    fun fillInstanceBuffer(arround: Location) {
        println("filling instance buffer")

        val buffer = MemoryUtil.memAlloc(instancesBuffer.bufferSize.toInt())

        val arroundi = arround.toVec3i()
        val radius = 10

        instances = 0
        for(x in arroundi.x - radius until arroundi.x + radius) {
            for(y in arroundi.y - radius until arroundi.y + radius) {
                for(z in arroundi.z - radius until arroundi.z + radius) {
                    val cell = client.world.peek(x, y, z)
                    //TODO cell.voxel never null
                    if(cell.voxel?.solid == true || java.lang.Math.random() > 0.99) {
                        buffer.putFloat(x.toFloat())
                        buffer.putFloat(y.toFloat())
                        buffer.putFloat(z.toFloat())
                        instances++
                    }
                }
            }
        }
        buffer.flip()
        instancesBuffer.upload(buffer)

        lastGenPosition = Vector3d(arround)

        MemoryUtil.memFree(buffer)
    }

    fun Entity.getCamera() : Camera {
        val fov = (90.0 / 360.0 * (Math.PI * 2)).toFloat()
        val aspect = backend.window.width.toFloat() / backend.window.height
        val projectionMatrix = Matrix4f().perspective(fov, aspect, 0.1f, 1000f, true)

        val up = Vector3f(0.0f, 1.0f, 0.0f)

        val cameraPosition = Vector3f(location.x.toFloat(), location.y.toFloat(), location.z.toFloat())

        //TODO this is a hack, use a trait to set this
        cameraPosition.y += 1.8f

        val entityDirection = (traits[TraitRotation::class]?.directionLookingAt ?: Vector3d(0.0, 0.0, 1.0)).toVec3f()
        val entityLookAt = Vector3f(cameraPosition).add(entityDirection)

        val viewMatrix = Matrix4f()
        viewMatrix.lookAt(cameraPosition, entityLookAt, up)

        val objectMatrix = Matrix4f()
        //objectMatrix.rotate((System.currentTimeMillis() % 3600000) * 0.001f, 0f , 1f, 0f)

        val modelViewMatrix = Matrix4f()
        modelViewMatrix.mul(viewMatrix)
        modelViewMatrix.mul(objectMatrix)

        return Camera(modelViewMatrix, projectionMatrix)
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        val entity = client.player.controlledEntity
        if(entity != null) {
            val camera = entity.getCamera()
            descriptorPool.configure(frame, camera)

            //println("$lastGenPosition ${lastGenPosition.distance(entity.location)}")

            if(lastGenPosition.distance(entity.location) > 10) {
                fillInstanceBuffer(entity.location)
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
