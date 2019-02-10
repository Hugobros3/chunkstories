package xyz.chunkstories.graphics.vulkan.systems

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.rendergraph.ImageInput
import xyz.chunkstories.api.graphics.rendergraph.ImageSource
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.world.getConditions
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D
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

    override fun registerAdditionalRenderTasks(passContext: VulkanFrameGraph.FrameGraphNode.PassNode) {
        if (doShadowMap) {
            val mainCamera = passContext.context.camera

            val rezs = floatArrayOf(512f, 256f, 64f, 16f)

            for(i in 0 until 4) {
                val rez = rezs[i]

                val shadowMapDepthRange = 256f
                val shadowMapExtent = rez

                val shadowMapContentsMatrix = Matrix4f().ortho(-shadowMapExtent, shadowMapExtent, -shadowMapExtent, shadowMapExtent, -shadowMapDepthRange, shadowMapDepthRange, true)
                val sunPosition = client.world.getConditions().sunPosition.toVec3f()

                val sunLookAt = Matrix4f().lookAt(sunPosition, Vector3f(0f), Vector3f(0f, 1f, 0f))

                val shadowMatrix = Matrix4f()
                shadowMatrix.mul(shadowMapContentsMatrix, shadowMatrix)
                shadowMatrix.mul(sunLookAt, shadowMatrix)

                shadowMatrix.translate(Vector3f(mainCamera.position).negate())

                val sunCamera = Camera(viewMatrix = shadowMatrix, fov = 0f, position = mainCamera.position)

                passContext.dispatchRenderTask("shadowmapCascade$i", sunCamera, "sunShadow", mapOf("shadowBuffer" to pass.renderTask.buffers["shadowBuffer$i"]!!)) {
                    val node = it as VulkanFrameGraph.FrameGraphNode.RenderingContextNode
                    //println("RESOLVED DB: ${node.rootPassInstance.resolvedDepthBuffer}")
                    passContext.markRenderBufferAsInput(node.rootPassInstance.resolvedDepthBuffer)
                }
            }
        }
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer, passContext: VulkanFrameGraph.FrameGraphNode.PassNode) {
        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

        for (input in pass.declaration.inputs?.imageInputs ?: emptyList<ImageInput>()) {
            val source = input.source
            when (source) {
                is ImageSource.RenderBufferReference -> {
                    bindingContext.bindTextureAndSampler(input.name, pass.renderTask.buffers[source.renderBufferName]?.texture!!, sampler)
                }
                is ImageSource.AssetReference -> TODO()
                is ImageSource.TextureReference -> TODO()
            }
        }

        //println("pass ${pass.name}  $bindings")
        bindings?.invoke(this, bindingContext)

        if (doShadowMap) {
            //println(passContext.extraInputRenderBuffers.map { it.texture.imageHandle })

            val shadowInfo = ShadowMappingInfo()
            shadowInfo.cascadesCount = 4
            for(i in 0 until 4)
                bindingContext.bindTextureAndSampler("shadowBuffers", backend.textures.get("logo.png") as VulkanTexture2D, samplerShadow, i)

            for(i in 0 until shadowInfo.cascadesCount) {
                val shadowSubcontext = passContext.context.artifacts["shadowmapCascade$i"] as VulkanFrameGraph.FrameGraphNode.RenderingContextNode
                bindingContext.bindTextureAndSampler("shadowBuffers", shadowSubcontext.rootPassInstance.resolvedDepthBuffer.texture, samplerShadow, i)
                shadowInfo.cameras[i] = shadowSubcontext.parameters["camera"] as Camera
                //println(shadowInfo.cameras[i].viewMatrix.hashCode())
            }

            bindingContext.bindUBO("shadowInfo", shadowInfo)
            bindingContext.bindUBO("camera", passContext.context.camera)
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