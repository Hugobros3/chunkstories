package xyz.chunkstories.graphics.vulkan.systems.world

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.common.world.*
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.shaders.bindShaderResources
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.textures.voxels.VulkanVoxelTexturesArray

private typealias VkChunkIR = MutableList<VulkanChunkRepresentation.Section>

class VulkanChunkRepresentationsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<VulkanChunkRepresentation, VkChunkIR>(backend) {
    override val representationName: String = VulkanChunkRepresentation::class.java.canonicalName

    private val cubesVertexInput = VertexInputConfiguration {
        var offset = 0

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }!!.location)
            format(VK_FORMAT_R8G8B8A8_UINT)
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
        offset += 4

        binding {
            binding(0)
            stride(offset)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }
    }

    val sampler = VulkanSampler(backend, tilingMode = TextureTilingMode.REPEAT)

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer<VkChunkIR>(pass), ChunksRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String

        override val system: VulkanDispatchingSystem<VulkanChunkRepresentation, VkChunkIR>
            get() = this@VulkanChunkRepresentationsDispatcher

        init {
            this.apply(initCode)
        }

        private val cubesProgram = backend.shaderFactory.createProgram(/* TODO */"cubes", ShaderCompilationParameters(outputs = pass.declaration.outputs))
        private val cubesPipeline = Pipeline(backend, cubesProgram, pass, cubesVertexInput, Primitive.POINTS, FaceCullingMode.CULL_BACK)

        private val meshesProgram = backend.shaderFactory.createProgram(/* TODO */shader, ShaderCompilationParameters(outputs = pass.declaration.outputs))
        private val meshesPipeline = Pipeline(backend, meshesProgram, pass, meshesVertexInputCfg, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

        val chunkInfoStruct = cubesProgram.glslProgram.instancedInputs.find { it.name == "chunkInfo" }!!.struct
        val structSize = chunkInfoStruct.size
        val sizeAligned16 = getStd140AlignedSizeForStruct(chunkInfoStruct)

        val maxChunksRendered = 4096
        val ssboBufferSize = (sizeAligned16 * maxChunksRendered).toLong()

        override fun registerDrawingCommands(frame: VulkanFrame, context: SystemExecutionContext, commandBuffer: VkCommandBuffer, work: VkChunkIR) {
            val client = backend.window.client.ingame ?: return

            val staticMeshes = work.mapNotNull { it.staticMesh }
            val cubes = work.mapNotNull { it.cubes }

            MemoryStack.stackPush()

            fun drawCubes() {
                val bindingContext = backend.descriptorMegapool.getBindingContext(cubesPipeline)

                val camera = context.passInstance.taskInstance.camera
                val world = client.world

                bindingContext.bindStructuredUBO("camera", camera)
                bindingContext.bindStructuredUBO("world", world.getConditions())

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, cubesPipeline.handle)

                //TODO pool those
                val ssboDataTest = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)

                val ssboStuff = MemoryUtil.memAlloc(ssboDataTest.bufferSize.toInt())
                var instance = 0
                val voxelTexturesArray = client.content.voxels.textures as VulkanVoxelTexturesArray
                bindingContext.bindTextureAndSampler("albedoTextures", voxelTexturesArray.albedoOnionTexture, sampler)
                bindingContext.bindInstancedInput("chunkInfo", ssboDataTest)

                val viewportSize = ViewportSize()
                viewportSize.size.set(context.passInstance.renderTargetSize)
                //viewportSize.size.set(pass.declaration.outputs.outputs.getOrNull(0)?.let { passContext.resolvedOutputs.get(it)?.textureSize } ?: passContext.resolvedDepthBuffer!!.textureSize)
                bindingContext.bindStructuredUBO("viewportSize", viewportSize)

                bindingContext.preDraw(commandBuffer)

                for (staticMesh in cubes) {
                    val chunkRepresentation = staticMesh.parent
                    vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(staticMesh.buffer.handle), MemoryStack.stackLongs(0))

                    val chunkRenderInfo = ChunkRenderInfo().apply {
                        chunkX = chunkRepresentation.chunk.chunkX
                        chunkY = chunkRepresentation.chunk.chunkY
                        chunkZ = chunkRepresentation.chunk.chunkZ
                    }

                    extractInterfaceBlock(ssboStuff, instance * sizeAligned16, chunkRenderInfo, chunkInfoStruct)

                    vkCmdDraw(commandBuffer, staticMesh.count, 1, 0, instance++)

                    frame.stats.totalVerticesDrawn += staticMesh.count
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
            }
            drawCubes()

            fun drawStaticMeshes() {
                val bindingContext = backend.descriptorMegapool.getBindingContext(meshesPipeline)

                val world = client.world
                val camera = context.passInstance.taskInstance.camera
                val cameraSectionX = section(camera.position.x(), world)
                val cameraSectionZ = section(camera.position.z(), world)

                bindingContext.bindStructuredUBO("camera", camera)
                bindingContext.bindStructuredUBO("world", world.getConditions())

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.handle)

                if (backend.logicalDevice.useGlobalTexturing)
                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.pipelineLayout, 0, MemoryStack.stackLongs(backend.textures.magicTexturing!!.theSet), null)

                //TODO pool those
                val ssboDataTest = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)

                val ssboStuff = MemoryUtil.memAlloc(ssboDataTest.bufferSize.toInt())
                var instance = 0
                val voxelTexturesArray = client.content.voxels.textures as VulkanVoxelTexturesArray
                bindingContext.bindTextureAndSampler("albedoTextures", voxelTexturesArray.albedoOnionTexture, sampler)
                bindingContext.bindInstancedInput("chunkInfo", ssboDataTest)

                context.bindShaderResources(bindingContext)

                bindingContext.preDraw(commandBuffer)

                for (staticMesh in staticMeshes) {
                    val chunkRepresentation = staticMesh.parent
                    vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(staticMesh.buffer.handle), MemoryStack.stackLongs(0))

                    val chunkRenderInfo = ChunkRenderInfo().apply {
                        var cx = chunkRepresentation.chunk.chunkX
                        var cz = chunkRepresentation.chunk.chunkZ

                        val cxSection = sectionChunk(cx, world)
                        val czSection = sectionChunk(cz, world)

                        cx += shouldWrap(cameraSectionX, cxSection) * world.sizeInChunks
                        cz += shouldWrap(cameraSectionZ, czSection) * world.sizeInChunks

                        chunkX = cx
                        chunkY = chunkRepresentation.chunk.chunkY
                        chunkZ = cz
                    }

                    extractInterfaceBlock(ssboStuff, instance * sizeAligned16, chunkRenderInfo, chunkInfoStruct)

                    vkCmdDraw(commandBuffer, staticMesh.count, 1, 0, instance++)

                    frame.stats.totalVerticesDrawn += staticMesh.count
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
            }
            drawStaticMeshes()

            MemoryStack.stackPop()
        }

        override fun cleanup() {
            meshesPipeline.cleanup()
            meshesProgram.cleanup()

            cubesPipeline.cleanup()
            cubesProgram.cleanup()
        }
    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer<VkChunkIR>.() -> Unit) =
            Drawer(pass, drawerInitCode)

    /*override fun sort(representation: VulkanChunkRepresentation, drawers: Array<VulkanDispatchingSystem.Drawer<*>>, outputs: List<MutableList<Any>>) {
        //TODO look at material/tag and decide where to send it
        for (section in representation.sections.values) {
            for ((index, drawer) in drawers.withIndex()) {
                if ((drawer as Drawer).materialTag == section.materialTag) {
                    outputs[index].add(section)
                }
            }
        }
    }*/

    override fun sort(representations: Sequence<VulkanChunkRepresentation>, drawers: List<VulkanDispatchingSystem.Drawer<VkChunkIR>>, workForDrawers: MutableMap<VulkanDispatchingSystem.Drawer<VkChunkIR>, VkChunkIR>) {
        val lists = drawers.associateWith { mutableListOf<VulkanChunkRepresentation.Section>() }

        for(representation in representations) {
            for (section in representation.sections.values) {
                for (drawer in drawers) {
                    if ((drawer as Drawer).materialTag == section.materialTag) {
                        lists[drawer]!!.add(section)
                    }
                }
            }
        }

        for(entry in lists) {
            if(entry.value.isNotEmpty()) {
                workForDrawers[entry.key] = entry.value
            }
        }
    }

    override fun cleanup() {
        sampler.cleanup()
    }

}