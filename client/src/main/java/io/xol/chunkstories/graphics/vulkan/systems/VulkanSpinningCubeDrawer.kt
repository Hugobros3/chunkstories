package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.graphics.Camera
import io.xol.chunkstories.api.voxel.VoxelSide
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class VulkanSpinningCubeDrawer(pass: VulkanPass) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    val guiShaderProgram = backend.shaderFactory.createProgram(backend, "/shaders/cube/cube")
    val descriptorPool = DescriptorPool(backend, guiShaderProgram)

    val pipeline = Pipeline(backend, pass, guiShaderProgram) {
        val bindingDescription = VkVertexInputBindingDescription.callocStack(1).apply {
            binding(0)
            stride(3 * 4 + 2 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        val attributeDescriptions = VkVertexInputAttributeDescription.callocStack(2)
        attributeDescriptions[0].apply {
            binding(0)
            location(guiShaderProgram.glslProgram.vertexInputs.find { it.name == "vertexIn" }?.location!! )
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(0)
        }

        attributeDescriptions[1].apply {
            binding(0)
            location(guiShaderProgram.glslProgram.vertexInputs.find { it.name == "texCoordIn" }?.location!! )
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(3 * 4)
        }

        VkPipelineVertexInputStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO).apply {
            pVertexBindingDescriptions(bindingDescription)
            pVertexAttributeDescriptions(attributeDescriptions)
        }
    }

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

        val camera = Camera(modelViewMatrix, projectionMatrix)

        descriptorPool.configure(frame, camera)
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 0, descriptorPool.setsForFrame(frame), null as? IntArray)

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
        vkCmdDraw(commandBuffer, 3 * 2 * 6, 1, 0, 0)

    }

    override fun cleanup() {
        vertexBuffer.cleanup()
        descriptorPool.cleanup()

        pipeline.cleanup()
        guiShaderProgram.cleanup()
    }
}