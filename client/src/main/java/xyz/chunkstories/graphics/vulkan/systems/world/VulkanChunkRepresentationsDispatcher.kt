package xyz.chunkstories.graphics.vulkan.systems.world

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.common.world.*
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
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

        override fun registerDrawingCommands(context: VulkanPassInstance, commandBuffer: VkCommandBuffer, work: VkChunkIR) {
            val client = backend.window.client.ingame ?: return

            val staticMeshes = work.mapNotNull { it.staticMesh }
            val cubes = work.mapNotNull { it.cubes }

            stackPush()

            fun drawCubes() {
                val bindingContext = context.getBindingContext(cubesPipeline)

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, cubesPipeline.handle)

                //TODO pool those
                val chunkInformationsStorageBuffer = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)

                val chunkInformations = memAlloc(chunkInformationsStorageBuffer.bufferSize.toInt())
                var instance = 0
                val voxelTexturesArray = client.content.voxels.textures as VulkanVoxelTexturesArray
                bindingContext.bindTextureAndSampler("albedoTextures", voxelTexturesArray.albedoOnionTexture, sampler)
                bindingContext.bindInstancedInput("chunkInfo", chunkInformationsStorageBuffer)

                bindingContext.commitAndBind(commandBuffer)

                for (staticMesh in cubes) {
                    val chunkRepresentation = staticMesh.parent
                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(staticMesh.buffer.handle), stackLongs(0))

                    val chunkRenderInfo = ChunkRenderInfo().apply {
                        chunkX = chunkRepresentation.chunk.chunkX
                        chunkY = chunkRepresentation.chunk.chunkY
                        chunkZ = chunkRepresentation.chunk.chunkZ
                    }

                    extractInterfaceBlock(chunkInformations, instance * sizeAligned16, chunkRenderInfo, chunkInfoStruct)

                    vkCmdDraw(commandBuffer, staticMesh.count, 1, 0, instance++)

                    context.frame.stats.totalVerticesDrawn += staticMesh.count
                    context.frame.stats.totalDrawcalls++
                }

                chunkInformations.position(instance * sizeAligned16)
                chunkInformations.flip()
                chunkInformationsStorageBuffer.upload(chunkInformations)
                memFree(chunkInformations)

                context.frame.recyclingTasks.add {
                    bindingContext.recycle()
                    chunkInformationsStorageBuffer.cleanup()//TODO recycle don't destroy!
                }
            }
            drawCubes()

            fun drawStaticMeshes() {
                val bindingContext = context.getBindingContext(meshesPipeline)

                val world = client.world
                val camera = context.taskInstance.camera
                val cameraSectionX = section(camera.position.x(), world)
                val cameraSectionZ = section(camera.position.z(), world)

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.handle)

                //TODO not here
                //if (backend.logicalDevice.useGlobalTexturing)
                //    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.pipelineLayout, 0, MemoryStack.stackLongs(backend.textures.magicTexturing!!.theSet), null)

                //TODO pool those
                val chunkInformationsStorageBuffer = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
                val chunkInformations = memAlloc(chunkInformationsStorageBuffer.bufferSize.toInt())
                val voxelTexturesArray = client.content.voxels.textures as VulkanVoxelTexturesArray
                bindingContext.bindTextureAndSampler("albedoTextures", voxelTexturesArray.albedoOnionTexture, sampler)
                bindingContext.bindInstancedInput("chunkInfo", chunkInformationsStorageBuffer)
                //context.bindShaderResources(bindingContext)
                bindingContext.commitAndBind(commandBuffer)

                var instancesCounter = 0
                for (staticMesh in staticMeshes) {
                    val chunkRepresentation = staticMesh.parent
                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(staticMesh.buffer.handle), stackLongs(0))

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

                    extractInterfaceBlock(chunkInformations, instancesCounter * sizeAligned16, chunkRenderInfo, chunkInfoStruct)

                    vkCmdDraw(commandBuffer, staticMesh.count, 1, 0, instancesCounter++)

                    context.frame.stats.totalVerticesDrawn += staticMesh.count
                    context.frame.stats.totalDrawcalls++
                }

                chunkInformations.position(instancesCounter * sizeAligned16)
                chunkInformations.flip()
                chunkInformationsStorageBuffer.upload(chunkInformations)
                memFree(chunkInformations)

                context.frame.recyclingTasks.add {
                    bindingContext.recycle()
                    chunkInformationsStorageBuffer.cleanup()//TODO recycle don't destroy!
                }
            }
            drawStaticMeshes()

            stackPop()
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