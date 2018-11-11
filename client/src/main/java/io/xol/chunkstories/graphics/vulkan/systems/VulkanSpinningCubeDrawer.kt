package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.graphics.structs.Camera
import io.xol.chunkstories.api.voxel.VoxelSide
import io.xol.chunkstories.graphics.common.Primitive
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class VulkanSpinningCubeDrawer(pass: VulkanPass) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    //val guiShaderProgram = backend.shaderFactory.createProgram(backend, "/shaders/cube/cube")
    val descriptorPool = DescriptorPool(backend, pass.program)

     val vertexInputConfiguration = vertexInputConfiguration {
         binding {
            binding(0)
            stride(3 * 4 + 2 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }?.location!! )
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(0)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "texCoordIn" }?.location!! )
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(3 * 4)
        }
    }

    val pipeline = Pipeline(backend, pass, vertexInputConfiguration, Primitive.TRIANGLES)

    private val vertexBuffer: VulkanVertexBuffer

    init {
        val vertices = floatArrayOf(
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

        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L)

        stackPush().use {
            val byteBuffer = stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        val fov = (90.0 / 360.0 * (Math.PI * 2)).toFloat()
        val aspect = backend.window.width.toFloat() / backend.window.height
        val projectionMatrix = Matrix4f().perspective(fov, aspect, 0.1f, 1000f, true)

        val up = Vector3f(0.0f, 1.0f, 0.0f)

        VoxelSide.FRONT

        val cubePosition = Vector3f(0f)
        val cameraPosition = Vector3f(0f, 0f, -5f)

        val viewMatrix = Matrix4f()
        viewMatrix.lookAt(cameraPosition, cubePosition, up)

        val objectMatrix = Matrix4f()
        objectMatrix.rotate((System.currentTimeMillis() % 3600000) * 0.001f, 0f , 1f, 0f)

        val modelViewMatrix = Matrix4f()
        modelViewMatrix.mul(viewMatrix)
        modelViewMatrix.mul(objectMatrix)

        val camera = Camera(cameraPosition, cubePosition, up, modelViewMatrix, projectionMatrix)

        descriptorPool.configure(frame, camera)
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 1, descriptorPool.setsForFrame(frame), null as? IntArray)

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
        vkCmdDraw(commandBuffer, 3 * 2 * 6, 1, 0, 0)

    }

    override fun cleanup() {
        vertexBuffer.cleanup()
        descriptorPool.cleanup()

        pipeline.cleanup()
    }
}