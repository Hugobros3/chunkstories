package xyz.chunkstories.graphics.vulkan.systems.dispatching.chunks

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.representations.RepresentationsGathered
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.common.world.ChunkRenderInfo
import xyz.chunkstories.graphics.common.world.section
import xyz.chunkstories.graphics.common.world.sectionChunk
import xyz.chunkstories.graphics.common.world.shouldWrap
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderTaskInstance
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.dispatching.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.textures.voxels.VulkanVoxelTexturesArray

class VulkanChunkRepresentationsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<VulkanChunkRepresentation>(backend) {
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

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer(pass), ChunksRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String

        override val system: VulkanDispatchingSystem<VulkanChunkRepresentation>
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

        override fun registerDrawingCommands(drawerWork: DrawerWork) {
            val work = drawerWork as ChunkMeshesDrawerWork
            val context = work.drawerInstance.first
            val commandBuffer = work.cmdBuffer

            val client = backend.window.client.ingame ?: return

            val staticMeshes = work.queuedWork.mapNotNull { it.staticMesh }
            val cubes = work.queuedWork.mapNotNull { it.cubes }

            stackPush()

            fun drawCubes() {
                val bindingContext = context.getBindingContext(cubesPipeline)

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, cubesPipeline.handle)

                val (chunkInformationsCpuBuffer, chunkInformationsGpuBuffer) = bindingContext.dataAllocator.getMappedSSBO(ssboBufferSize)

                val voxelTexturesArray = client.content.voxels.textures as VulkanVoxelTexturesArray
                bindingContext.bindTextureAndSampler("albedoTextures", voxelTexturesArray.albedoOnionTexture, sampler)
                bindingContext.bindInstancedInput("chunkInfo", chunkInformationsGpuBuffer.first, chunkInformationsGpuBuffer.second)

                bindingContext.commitAndBind(commandBuffer)

                var instancesCounter = 0
                for (staticMesh in cubes) {
                    val chunkRepresentation = staticMesh.parent
                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(staticMesh.buffer.handle), stackLongs(0))

                    val chunkRenderInfo = ChunkRenderInfo().apply {
                        chunkX = chunkRepresentation.chunk.chunkX
                        chunkY = chunkRepresentation.chunk.chunkY
                        chunkZ = chunkRepresentation.chunk.chunkZ
                    }

                    extractInterfaceBlock(chunkInformationsCpuBuffer, instancesCounter * sizeAligned16, chunkRenderInfo, chunkInfoStruct)

                    vkCmdDraw(commandBuffer, staticMesh.count, 1, 0, instancesCounter++)

                    context.frame.stats.totalVerticesDrawn += staticMesh.count
                    context.frame.stats.totalDrawcalls++
                }

                chunkInformationsCpuBuffer.position(instancesCounter * sizeAligned16)
                chunkInformationsCpuBuffer.flip()

                context.frame.recyclingTasks.add {
                    bindingContext.recycle()
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

                val (chunkInformationsCpuBuffer, chunkInformationsGpuBuffer) = bindingContext.dataAllocator.getMappedSSBO(ssboBufferSize)

                val voxelTexturesArray = client.content.voxels.textures as VulkanVoxelTexturesArray
                bindingContext.bindTextureAndSampler("albedoTextures", voxelTexturesArray.albedoOnionTexture, sampler)
                bindingContext.bindInstancedInput("chunkInfo", chunkInformationsGpuBuffer.first, chunkInformationsGpuBuffer.second)

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

                    extractInterfaceBlock(chunkInformationsCpuBuffer, instancesCounter * sizeAligned16, chunkRenderInfo, chunkInfoStruct)

                    vkCmdDraw(commandBuffer, staticMesh.count, 1, 0, instancesCounter++)

                    context.frame.stats.totalVerticesDrawn += staticMesh.count
                    context.frame.stats.totalDrawcalls++
                }

                chunkInformationsCpuBuffer.position(instancesCounter * sizeAligned16)
                chunkInformationsCpuBuffer.flip()
                //chunkInformationsStorageBuffer.upload(chunkInformations)
                //memFree(chunkInformations)

                context.frame.recyclingTasks.add {
                    bindingContext.recycle()
                    //chunkInformationsStorageBuffer.cleanup()//TODO recycle don't destroy!
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

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer.() -> Unit) = Drawer(pass, drawerInitCode)

    class ChunkMeshesDrawerWork(drawerInstance: Pair<VulkanPassInstance, Drawer>): DrawerWork(drawerInstance) {
        val queuedWork = arrayListOf<VulkanChunkRepresentation.Section>()
        override fun isEmpty() = queuedWork.isEmpty()
    }

    override fun sortWork(frame: VulkanFrame, drawers: Map<VulkanRenderTaskInstance, List<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>>>, maskedBuckets: Map<Int, RepresentationsGathered.Bucket>): Map<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>, DrawerWork> {
        @Suppress("UNCHECKED_CAST") val allDrawersPlusInstances = drawers.values.flatten().filterIsInstance<Pair<VulkanPassInstance, Drawer>>()

        val workForDrawers = allDrawersPlusInstances.associateWith {
            ChunkMeshesDrawerWork(it)
        }

        for ((mask, bucket) in maskedBuckets) {
            val drawerRelevancyMap = mutableMapOf<String, List<ChunkMeshesDrawerWork>>()

            @Suppress("UNCHECKED_CAST") val somewhatRelevantDrawers = drawers.filter { it.key.mask and mask != 0 }.flatMap { it.value } as List<Pair<VulkanPassInstance, Drawer>>
            @Suppress("UNCHECKED_CAST") val representations = bucket.representations as ArrayList<VulkanChunkRepresentation>

            for (chunkRepresentation in representations) {
                for ((material, section) in chunkRepresentation.sections) {
                    val relevantWorkQueues = drawerRelevancyMap.getOrPut(material) {
                        somewhatRelevantDrawers.filter { it.second.materialTag == material }.map { workForDrawers[it]!! }
                    }

                    for (queue in relevantWorkQueues) {
                        queue.queuedWork.add(section)
                    }
                }
            }
        }

        return workForDrawers.map { Pair(it.key, it.value) }.toMap()
    }

    override fun cleanup() {
        sampler.cleanup()
    }

}