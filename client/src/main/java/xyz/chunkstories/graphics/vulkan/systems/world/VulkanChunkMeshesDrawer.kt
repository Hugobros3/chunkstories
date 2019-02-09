package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.structs.WorldConditions
import xyz.chunkstories.api.util.kotlin.toVec3i
import xyz.chunkstories.api.world.World
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.world.ChunkRenderInfo
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import xyz.chunkstories.world.WorldClientCommon
import xyz.chunkstories.world.chunk.CubicChunk
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.region.Region
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.extractInterfaceBlockField
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.world.storage.RegionImplementation
import java.util.*

class VulkanCubesDrawer(pass: VulkanPass, val client: IngameClient) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    private val meshesVertexInputCfg = VertexInputConfiguration {
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

    val cubesProgram = backend.shaderFactory.createProgram("cubes")
    private val meshesPipeline = Pipeline(backend, cubesProgram, pass, meshesVertexInputCfg, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

    companion object {
        var totalCubesDrawn = 0
        var totalBuffersUsed = 0
    }

    val chunkInfoID = cubesProgram.glslProgram.instancedInputs.find { it.name == "chunkInfo" }!!
    val structSize = chunkInfoID.struct.size
    val sizeAligned16 = if(structSize % 16 == 0) structSize else (structSize / 16 * 16) + 16

    val sizeFor2048Elements = sizeAligned16 * 2048L

    private val ssboDataTest = InflightFrameResource(backend) {
        VulkanBuffer(backend, sizeFor2048Elements, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, false)
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer, passContext: VulkanFrameGraph.FrameGraphNode.PassNode) {
        MemoryStack.stackPush()

        val bindingContext = backend.descriptorMegapool.getBindingContext(meshesPipeline)

        val camera = passContext.context.camera
        val world = client.world as WorldClientCommon

        val camPos = camera.position

        bindingContext.bindUBO("camera", camera)
        bindingContext.bindUBO("world", world.getConditions())

        //val frustrum = Frustrum(camera, client.gameWindow)

        totalCubesDrawn = 0
        totalBuffersUsed = 0

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.handle)
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.pipelineLayout, 0, stackLongs(backend.textures.magicTexturing.theSet), null)

        val camChunk = camPos.toVec3i()
        camChunk.x /= 32
        camChunk.y /= 32
        camChunk.z /= 32

        val drawDistance = world.client.configuration.getIntValue(InternalClientOptions.viewDistance) / 32
        val drawDistanceH = 6

        val usedData = mutableListOf<ChunkVkMeshProperty.ChunkVulkanMeshData>()

        val ssboStuff = memAlloc(ssboDataTest[frame].bufferSize.toInt())
        var instance = 0
        bindingContext.bindSSBO("chunkInfo", ssboDataTest[frame])
        bindingContext.preDraw(commandBuffer)

        fun renderChunk(chunk: CubicChunk) {

            if (chunk.meshData is ChunkVkMeshProperty) {
                val block = (chunk.meshData as ChunkVkMeshProperty).get()
                if (block != null)
                    usedData.add(block)

                if (block?.vertexBuffer != null) {
                    //vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.layout, 0, stackLongs(block.virtualTexturingContext!!.setHandle), null)
                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(block.vertexBuffer.handle), stackLongs(0))

                    /*if (block.perChunkBindings == null || block.perChunkBindings!!.pipeline !== meshesPipeline) {
                        val chunkRenderInfo = ChunkRenderInfo().apply {
                            chunkX = chunk.chunkX
                            chunkY = chunk.chunkY
                            chunkZ = chunk.chunkZ
                        }
                        block.perChunkBindings = backend.descriptorMegapool.getBindingContext(meshesPipeline).also {
                            it.bindUBO(chunkRenderInfo)
                        }
                    }
                    block.perChunkBindings!!.preDraw(commandBuffer)*/

                    ssboStuff.position(instance * sizeAligned16)
                    val chunkRenderInfo = ChunkRenderInfo().apply {
                        chunkX = chunk.chunkX
                        chunkY = chunk.chunkY
                        chunkZ = chunk.chunkZ
                    }

                    for (field in chunkInfoID.struct.fields) {
                        ssboStuff.position(instance * sizeAligned16 + field.offset)
                        extractInterfaceBlockField(field, ssboStuff, chunkRenderInfo)
                    }

                    vkCmdDraw(commandBuffer, block.count, 1, 0, instance++)

                    totalCubesDrawn += block.count
                    totalBuffersUsed++
                }
            } else {
                // This avoids the condition where the meshData is created after the chunk is destroyed
                chunk.chunkDestructionSemaphore.acquireUninterruptibly()
                if (!chunk.isDestroyed)
                    chunk.meshData = ChunkVkMeshProperty(backend, chunk)
                chunk.chunkDestructionSemaphore.release()
            }
        }

        val boxCenter = Vector3f(0f)
        val boxSize = Vector3f(32f, 32f, 32f)
        val boxSize2 = Vector3f(256f, 256f, 256f)

        val sortedChunks = ArrayList<CubicChunk>()

        val visibleRegions = arrayOfNulls<RegionImplementation>(1024)
        var visibleRegionsCount = 0

        //val unsortedRegions = ArrayList<RegionImplementation>()

        var rc = 0
        for (region in world.allLoadedRegions) {
            boxCenter.x = region.regionX * 256.0f + 128.0f
            boxCenter.y = region.regionY * 256.0f + 128.0f
            boxCenter.z = region.regionZ * 256.0f + 128.0f

            rc++

            if(camera.frustrum.isBoxInFrustrum(boxCenter, boxSize2)) {
                visibleRegions[visibleRegionsCount++] = region as RegionImplementation
            }
        }

        Arrays.sort(visibleRegions, 0, visibleRegionsCount) { a, b ->
            fun distSquared(r: Region) : Float {
                val rcx = r.regionX * 256.0f + 128.0f
                val rcy = r.regionY * 256.0f + 128.0f
                val rcz = r.regionZ * 256.0f + 128.0f

                val dx = camPos.x() - rcx
                val dy = camPos.y() - rcy
                val dz = camPos.z() - rcz

                return dx * dx + dy * dy + dz * dz
            }

            (distSquared(a!!) - distSquared(b!!)).toInt()
        }

        val visibleRegionChunks = arrayOfNulls<CubicChunk>(8 * 8 * 8)
        var visibleRegionChunksCount : Int

        val visibilityRangeX = (camChunk.x - drawDistance)..(camChunk.x + drawDistance)
        val visibilityRangeY = (camChunk.y - drawDistanceH)..(camChunk.y + drawDistanceH)
        val visibilityRangeZ = (camChunk.z - drawDistance)..(camChunk.z + drawDistance)

        for(i in 0 until visibleRegionsCount) {
            val region = visibleRegions[i]!!

            visibleRegionChunksCount = 0
            for (chunk in region.loadedChunks) {
                boxCenter.x = chunk.chunkX * 32.0f + 16.0f
                boxCenter.y = chunk.chunkY * 32.0f + 16.0f
                boxCenter.z = chunk.chunkZ * 32.0f + 16.0f

                if(!chunk.isAirChunk) {
                    if(chunk.chunkX in visibilityRangeX && chunk.chunkY in visibilityRangeY && chunk.chunkZ in visibilityRangeZ) {

                        if (camera.frustrum.isBoxInFrustrum(boxCenter, boxSize)) {
                            visibleRegionChunks[visibleRegionChunksCount++] = chunk
                            //sortedChunks.add(chunk)
                        }
                    }
                }
            }

            Arrays.sort(visibleRegionChunks, 0, visibleRegionChunksCount) { a, b ->
                fun distSquared(c: Chunk) : Float {
                    val ccx = c.chunkX * 32.0f + 16.0f
                    val ccy = c.chunkY * 32.0f + 16.0f
                    val ccz = c.chunkZ * 32.0f + 16.0f

                    val dx = camPos.x() - ccx
                    val dy = camPos.y() - ccy
                    val dz = camPos.z() - ccz

                    return dx * dx + dy * dy + dz * dz
                }

                (distSquared(a!!) - distSquared(b!!)).toInt()
            }

            for(j in 0 until visibleRegionChunksCount) {
                renderChunk(visibleRegionChunks[j]!!)
            }
        }

        ssboStuff.flip()
        ssboDataTest[frame].upload(ssboStuff)
        memFree(ssboStuff)

        frame.recyclingTasks.add {
            usedData.forEach(ChunkVkMeshProperty.ChunkVulkanMeshData::release)
            bindingContext.recycle()
        }

        MemoryStack.stackPop()
    }

    override fun cleanup() {
        meshesPipeline.cleanup()
        cubesProgram.cleanup()
    }
}

fun World.getConditions(): WorldConditions {
    //val time = System.currentTimeMillis()
    val dayCycle = (time % 10000L) / 10000f
    val sunPos = Vector3d(0.5, -1.0, 0.0)
    if (dayCycle > 0f)
        sunPos.rotateAbout(dayCycle * Math.PI * 2.0, 1.0, 0.0, 0.0).normalize()
    //val sunPos = Vector3d(0.0, 1.0, 0.0).normalize()
    return WorldConditions(
            time = dayCycle,
            sunPosition = sunPos,
            cloudyness = this.weather
    )
}
