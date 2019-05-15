package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.representation.Sprite
import xyz.chunkstories.api.graphics.systems.dispatching.SpritesRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.shaders.GLSLInstancedInput
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.world.WorldClientCommon

class VulkanSpritesDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<Sprite>(backend) {

    override val representationName: String = Sprite::class.java.canonicalName

    val sampler = VulkanSampler(backend)

    val vertexInputs = VertexInputConfiguration {
        attribute {
            binding(0)
            location(0)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(0)
        }

        binding {
            binding(0)
            stride(8)
            inputRate()
        }
    }

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer<Sprite>(pass), SpritesRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String

        override val system: VulkanDispatchingSystem<Sprite>
            get() = this@VulkanSpritesDispatcher

        val program: VulkanShaderProgram
        val pipeline: Pipeline

        val vertexBuffer: VulkanVertexBuffer

        val spriteII: GLSLInstancedInput
        val instanceDataPaddedSize: Int

        init {
            this.apply(initCode)

            program = backend.shaderFactory.createProgram(shader, ShaderCompilationParameters(outputs = pass.declaration.outputs))
            pipeline = Pipeline(backend, program, pass, vertexInputs, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

            val vertices = floatArrayOf(
                    -1.0F, -1.0F,
                    1.0F, 1.0F,
                    -1.0F, 1.0F,
                    -1.0F, -1.0F,
                    1.0F, -1.0F,
                    1.0F, 1.0F
            )

            vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L, MemoryUsagePattern.STATIC)

            spriteII = pipeline.program.glslProgram.instancedInputs.find { it.name == "sprite" }!!
            instanceDataPaddedSize = getStd140AlignedSizeForStruct(spriteII.struct)

            MemoryStack.stackPush().use {
                val byteBuffer = MemoryStack.stackMalloc(vertices.size * 4)
                vertices.forEach { f -> byteBuffer.putFloat(f) }
                byteBuffer.flip()

                vertexBuffer.upload(byteBuffer)
            }
        }

        val ssboBufferSize = 1024 * 1024L

        override fun registerDrawingCommands(frame: VulkanFrame, ctx: SystemExecutionContext, commandBuffer: VkCommandBuffer, work: Sequence<Sprite>) {
            MemoryStack.stackPush()

            val client = backend.window.client.ingame ?: return

            val bindingContexts = mutableListOf<DescriptorSetsMegapool.ShaderBindingContext>()

            val instancesSSBO = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
            val instancesBuffer = MemoryUtil.memAlloc(instancesSSBO.bufferSize.toInt())
            var instance = 0

            val camera = ctx.passInstance.taskInstance.camera
            val world = client.world as WorldClientCommon

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))

            for (sprite in work) {
                val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
                bindingContexts.add(bindingContext)

                extractInterfaceBlock(instancesBuffer, instance * instanceDataPaddedSize, sprite, spriteII.struct)

                val material = sprite.material

                for (materialImageSlot in pipeline.program.glslProgram.materialImages) {
                    val textureName = material.textures[materialImageSlot.name] ?: "textures/notex.png"
                    bindingContext.bindTextureAndSampler(materialImageSlot.name, backend.textures.getOrLoadTexture2D(textureName), sampler)
                    //println(pipeline.program.glslProgram)
                }

                bindingContext.bindUBO("camera", camera)
                bindingContext.bindUBO("world", world.getConditions())

                bindingContext.bindSSBO("sprite", instancesSSBO)

                bindingContext.preDraw(commandBuffer)
                vkCmdDraw(commandBuffer, 3 * 2, 1, 0, instance++)
            }

            instancesBuffer.position(instance * instanceDataPaddedSize)
            instancesBuffer.flip()

            instancesSSBO.upload(instancesBuffer)

            memFree(instancesBuffer)

            frame.recyclingTasks.add {
                bindingContexts.forEach { it.recycle() }
                instancesSSBO.cleanup()
            }

            MemoryStack.stackPop()
        }

        override fun cleanup() {
            pipeline.cleanup()
            program.cleanup()
            vertexBuffer.cleanup()
        }
    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer<*>.() -> Unit) = Drawer(pass, drawerInitCode)

    override fun sort(instance: Sprite, drawers: Array<VulkanDispatchingSystem.Drawer<*>>, outputs: List<MutableList<Any>>) {

        //val meshInstance = MeshInstance(mesh, instance.materials[i] ?: mesh.material, instance)

        for ((index, drawer) in drawers.withIndex()) {
            if ((drawer as VulkanSpritesDispatcher.Drawer).materialTag == instance.material.tag) {
                outputs[index].add(instance)
            }
        }

    }

    override fun cleanup() {
        sampler.cleanup()
    }
}