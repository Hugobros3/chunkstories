package xyz.chunkstories.graphics.vulkan.systems

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.ImageInput
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.FrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.world.getConditions
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.util.ShadowMappingInfo
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration


class VulkanFullscreenQuadDrawer(pass: VulkanPass) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    val client: IngameClient
        get() = backend.window.client.ingame!!

    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding(0)
            stride(2 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }?.location!!)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(0)
        }
    }

    val program = backend.shaderFactory.createProgram(pass.declaration.name)
    val pipeline = Pipeline(backend, program, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)
    val sampler = VulkanSampler(backend, false)
    val samplerShadow = VulkanSampler(backend, true)

    private val vertexBuffer: VulkanVertexBuffer

    init {
        val vertices = floatArrayOf(
                -1.0F, -3.0F,
                3.0F, 1.0F,
                -1.0F, 1.0F
        )

        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L)

        stackPush().use {
            val byteBuffer = stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    var bindings: (VulkanFullscreenQuadDrawer.(DescriptorSetsMegapool.ShaderBindingContext) -> Unit)? = null

    var doShadowMap = false

    fun shaderBindings(bindings: VulkanFullscreenQuadDrawer.(bindingContext: DescriptorSetsMegapool.ShaderBindingContext) -> Unit) {
        this.bindings = bindings
    }

    override fun registerAdditionalRenderTasks(passContext: FrameGraph.FrameGraphNode.PassNode) {
        if (doShadowMap) {
            val mainCamera = passContext.taskNode.camera

            val shadowMapDepthRange = 256f
            val shadowMapExtent = 256f

            val shadowMapContentsMatrix = Matrix4f().ortho(-shadowMapExtent, shadowMapExtent, -shadowMapExtent, shadowMapExtent, -shadowMapDepthRange, shadowMapDepthRange, true)
            //val sunLookAt = Matrix4f().lookAt(Vector3f(0f), client.world.getConditions().sunPosition.toVec3f().negate(), Vector3f(0f, 1f, 0f))
            val sunPosition = client.world.getConditions().sunPosition.toVec3f()

            /*val t = sunPosition.x
            sunPosition.x = sunPosition.z
            sunPosition.z = t
            sunPosition.x *= -1*/

            val sunLookAt = Matrix4f().lookAt(sunPosition, Vector3f(0f), Vector3f(0f, 1f, 0f))
            //println(client.world.getConditions().sunPosition.toVec3f())
            //println(client.world.getConditions().sunPosition)

            var shadowMatrix = Matrix4f()
            shadowMatrix.mul(shadowMapContentsMatrix, shadowMatrix)
            //shadowMatrix.translate(Vector3f(0f, 0f, -shadowMapDepthRange))
            shadowMatrix.mul(sunLookAt, shadowMatrix)
            //sunLookAt.mul(shadowMapContentsMatrix, shadowMatrix)
            //shadowMatrix = sunLookAt

            shadowMatrix.translate(Vector3f(mainCamera.position).negate())

            val sunCamera = Camera(viewMatrix = shadowMatrix, fov = 0f, position = mainCamera.position)

            passContext.dispatchRenderTask("shadowmapCascade0", sunCamera, "sunShadow", emptyMap()) {
                passContext.markRenderBufferAsInput(it.renderTask.rootPass.outputDepthRenderBuffer!!)
            }
        }
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer, passContext: FrameGraph.FrameGraphNode.PassNode) {
        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

        for (input in pass.declaration.inputs?.imageInputs ?: emptyList<ImageInput>()) {
            val source = input.source
            when (source) {
                is ImageInput.ImageSource.RenderBufferReference -> {
                    bindingContext.bindTextureAndSampler(input.name, pass.renderTask.buffers[source.renderBufferName]?.texture!!, sampler)
                }
                is ImageInput.ImageSource.AssetReference -> TODO()
                is ImageInput.ImageSource.TextureReference -> TODO()
            }
        }

        //println("pass ${pass.name}  $bindings")
        bindings?.invoke(this, bindingContext)

        if(doShadowMap) {
            val shadowSubctx0 = passContext.taskNode.artifacts["shadowmapCascade0"] as FrameGraph.FrameGraphNode.RenderingContextNode
            val shadowCamera0 = shadowSubctx0.parameters["camera"] as Camera

            val shadowInfo = ShadowMappingInfo()
            shadowInfo.cameras[0] = shadowCamera0
            shadowInfo.cascadesCount = 1

            bindingContext.bindUBO("shadowInfo", shadowInfo)
            bindingContext.bindTextureAndSampler("shadowBuffers", shadowSubctx0.renderTask.rootPass.outputDepthRenderBuffer!!.texture, samplerShadow, 0)
        }

        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
        bindingContext.preDraw(commandBuffer)
        vkCmdDraw(commandBuffer, 3 * 1, 1, 0, 0)

        frame.recyclingTasks.add {
            bindingContext.recycle()
        }
    }

    override fun cleanup() {
        sampler.cleanup()
        samplerShadow.cleanup()

        vertexBuffer.cleanup()
        pipeline.cleanup()
        program.cleanup()
        //descriptorPool.cleanup()
    }
}