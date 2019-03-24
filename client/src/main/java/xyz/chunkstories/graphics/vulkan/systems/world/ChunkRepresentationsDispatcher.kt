package xyz.chunkstories.graphics.vulkan.systems.world

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.world.ChunkRenderInfo
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.extractInterfaceBlockField
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.textures.voxels.VulkanVoxelTexturesArray
import xyz.chunkstories.world.WorldClientCommon

class ChunkRepresentationsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<ChunkRepresentation>(backend) {

    override val representationName: String = ChunkRepresentation::class.java.canonicalName

    private val meshesVertexInputCfg = VertexInputConfiguration {
        var offset = 0

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }!!.location)
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(offset)
        }
        offset += 4 * 3

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "colorIn" }!!.location)
            format(VK_FORMAT_R8G8B8A8_UNORM)
            offset(offset)
        }
        offset += 4

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "normalIn" }!!.location)
            format(VK_FORMAT_R8G8B8A8_SNORM)
            offset(offset)
        }
        offset += 4

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "texCoordIn" }!!.location)
            format(VK_FORMAT_R16G16_UNORM)
            offset(offset)
        }
        offset += 4

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "textureIdIn" }!!.location)
            format(VK_FORMAT_R32_UINT)
            offset(offset)
        }
        offset += 4

        binding {
            binding(0)
            stride(offset)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }
    }

    val sampler = VulkanSampler(backend, tilingMode = TextureTilingMode.REPEAT)

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer<ChunkRepresentation.Section>(pass), ChunksRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String

        override val system: VulkanDispatchingSystem<ChunkRepresentation>
            get() = this@ChunkRepresentationsDispatcher

        init {
            this.apply(initCode)
        }

        val cubesProgram = backend.shaderFactory.createProgram(shader, ShaderCompilationParameters(outputs = pass.declaration.outputs))
        private val meshesPipeline = Pipeline(backend, cubesProgram, pass, meshesVertexInputCfg, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

        val chunkInfoID = cubesProgram.glslProgram.instancedInputs.find { it.name == "chunkInfo" }!!
        val structSize = chunkInfoID.struct.size
        val sizeAligned16 = if (structSize % 16 == 0) structSize else (structSize / 16 * 16) + 16

        val maxChunksRendered = 4096
        val ssboBufferSize = (sizeAligned16 * maxChunksRendered).toLong()

        override fun registerDrawingCommands(frame: Frame, passContext: VulkanFrameGraph.FrameGraphNode.PassNode, commandBuffer: VkCommandBuffer, work: Sequence<ChunkRepresentation.Section>) {
            val client = backend.window.client.ingame ?: return

            MemoryStack.stackPush()

            val bindingContext = backend.descriptorMegapool.getBindingContext(meshesPipeline)

            val camera = passContext.context.camera
            val world = client.world

            bindingContext.bindUBO("camera", camera)
            bindingContext.bindUBO("world", world.getConditions())

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.handle)

            if (backend.logicalDevice.enableMagicTexturing)
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.pipelineLayout, 0, MemoryStack.stackLongs(backend.textures.magicTexturing!!.theSet), null)

            //TODO pool those
            val ssboDataTest = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)

            val ssboStuff = MemoryUtil.memAlloc(ssboDataTest.bufferSize.toInt())
            var instance = 0
            val voxelTexturesArray = client.content.voxels().textures() as VulkanVoxelTexturesArray
            bindingContext.bindTextureAndSampler("albedoTextures", voxelTexturesArray.albedoOnionTexture, sampler)
            bindingContext.bindSSBO("chunkInfo", ssboDataTest)

            if (shader == "water") {
                bindingContext.bindTextureAndSampler("waterNormalDeep", backend.textures.getOrLoadTexture2D("textures/water/deep.png"), sampler)
                bindingContext.bindTextureAndSampler("waterNormalShallow", backend.textures.getOrLoadTexture2D("textures/water/shallow.png"), sampler)
            }

            bindingContext.preDraw(commandBuffer)

            for (section in work) {
                val chunkRepresentation = section.parent
                vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(section.buffer.handle), MemoryStack.stackLongs(0))

                ssboStuff.position(instance * sizeAligned16)
                val chunkRenderInfo = ChunkRenderInfo().apply {
                    chunkX = chunkRepresentation.chunk.chunkX
                    chunkY = chunkRepresentation.chunk.chunkY
                    chunkZ = chunkRepresentation.chunk.chunkZ
                }

                for (field in chunkInfoID.struct.fields) {
                    ssboStuff.position(instance * sizeAligned16 + field.offset)
                    extractInterfaceBlockField(field, ssboStuff, chunkRenderInfo)
                }

                vkCmdDraw(commandBuffer, section.count, 1, 0, instance++)

                frame.stats.totalVerticesDrawn += section.count
                frame.stats.totalDrawcalls++
            }

            ssboStuff.position(instance * sizeAligned16)
            ssboStuff.flip()
            ssboDataTest.upload(ssboStuff)
            MemoryUtil.memFree(ssboStuff)

            frame.recyclingTasks.add {
                bindingContext.recycle()
                ssboDataTest.cleanup()//TODO recycle don't destroy!
            }

            MemoryStack.stackPop()
        }

        override fun cleanup() {
            meshesPipeline.cleanup()
            cubesProgram.cleanup()
        }
    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer<*>.() -> Unit) =
            Drawer(pass, drawerInitCode)

    override fun sort(representation: ChunkRepresentation, drawers: Array<VulkanDispatchingSystem.Drawer<*>>, outputs: List<MutableList<Any>>) {
        //TODO look at material/tag and decide where to send it
        for (section in representation.sections.values) {
            for ((index, drawer) in drawers.withIndex()) {
                if ((drawer as ChunkRepresentationsDispatcher.Drawer).materialTag == section.materialTag) {
                    outputs[index].add(section)
                }
            }
        }
    }

    override fun cleanup() {
        sampler.cleanup()
    }

}