package xyz.chunkstories.graphics.vulkan.systems

import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern

class VulkanSpinningCubeDrawer(pass: VulkanPass, dslCode: VulkanSpinningCubeDrawer.() -> Unit) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

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


    val program = backend.shaderFactory.createProgram("cube")
    val pipeline = Pipeline(backend, program, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

    private val vertexBuffer: VulkanVertexBuffer

    init {
        dslCode()

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

        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L, MemoryUsagePattern.STATIC)

        stackPush().use {
            val byteBuffer = stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun registerDrawingCommands(frame: VulkanFrame, ctx: SystemExecutionContext, commandBuffer: VkCommandBuffer) {
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

        val camera = Camera(cameraPosition, cubePosition, up, fov, modelViewMatrix, projectionMatrix)

        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)

        bindingContext.bindUBO("camera", camera)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

        bindingContext.preDraw(commandBuffer)
        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
        vkCmdDraw(commandBuffer, 3 * 2 * 6, 1, 0, 0)

        frame.recyclingTasks.add {
            bindingContext.recycle()
        }
     }

    override fun cleanup() {
        vertexBuffer.cleanup()

        pipeline.cleanup()
        program.cleanup()
    }
}