package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.WorldConditions
import xyz.chunkstories.api.physics.Frustrum
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
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.region.Region
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

    private val meshesPipeline = Pipeline(backend, pass, backend.shaderFactory.createProgram("cubes"), meshesVertexInputCfg, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

    companion object {
        var totalCubesDrawn = 0
        var totalBuffersUsed = 0
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        MemoryStack.stackPush()

        val bindingContext = backend.descriptorMegapool.getBindingContext(meshesPipeline)

        val entity = client.player.controlledEntity
        val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val world = client.world as WorldClientCommon

        val camPos = camera.position

        bindingContext.bindUBO(camera)
        bindingContext.bindUBO(world.getConditions())
        bindingContext.preDraw(commandBuffer)

        val frustrum = Frustrum(camera, client.gameWindow)

        totalCubesDrawn = 0
        totalBuffersUsed = 0

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.handle)
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.layout, 0, stackLongs(backend.textures.magicTexturing.theSet), null)

        val camChunk = camPos.toVec3i()
        camChunk.x /= 32
        camChunk.y /= 32
        camChunk.z /= 32

        val drawDistance = world.client.configuration.getIntValue(InternalClientOptions.viewDistance) / 32
        val drawDistanceH = 6

        val usedData = mutableListOf<ChunkVkMeshProperty.ChunkVulkanMeshData>()

        fun renderChunk(chunk: CubicChunk) {

            if (chunk.meshData is ChunkVkMeshProperty) {
                val block = (chunk.meshData as ChunkVkMeshProperty).get()
                if (block != null)
                    usedData.add(block)

                if (block?.vertexBuffer != null) {
                    //vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshesPipeline.layout, 0, stackLongs(block.virtualTexturingContext!!.setHandle), null)
                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(block.vertexBuffer.handle), stackLongs(0))

                    if (block.perChunkBindings == null || block.perChunkBindings!!.pipeline !== meshesPipeline) {
                        val chunkRenderInfo = ChunkRenderInfo().apply {
                            chunkX = chunk.chunkX
                            chunkY = chunk.chunkY
                            chunkZ = chunk.chunkZ
                        }
                        block.perChunkBindings = backend.descriptorMegapool.getBindingContext(meshesPipeline).also {
                            it.bindUBO(chunkRenderInfo)
                        }
                    }

                    block.perChunkBindings!!.preDraw(commandBuffer)

                    //vkCmdDraw(commandBuffer, 3 * 2 * 6, block.count, 0, 0)
                    vkCmdDraw(commandBuffer, block.count, 1, 0, 0)

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

            if(frustrum.isBoxInFrustrum(boxCenter, boxSize2)) {
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

                        if (frustrum.isBoxInFrustrum(boxCenter, boxSize)) {
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

        frame.recyclingTasks.add {
            usedData.forEach(ChunkVkMeshProperty.ChunkVulkanMeshData::release)
            bindingContext.recycle()
        }

        MemoryStack.stackPop()
    }

    override fun cleanup() {
        meshesPipeline.cleanup()
    }
}

fun World.getConditions(): WorldConditions {
    //val time = (System.currentTimeMillis() % 100000L) / 2L
    val time01 = (time % 10000L) / 10000f
    val sunPos = Vector3d(0.5, -1.0, 0.0)
    if (time01 > 0f)
        sunPos.rotateAbout(time01 * Math.PI * 2.0, 1.0, 0.0, 0.0).normalize()
    //println("${time01.toString()}:${sunPos.x} ${sunPos.y} + ${sunPos.z}")
    return WorldConditions(
            time = time01,
            sunPosition = sunPos,
            cloudyness = this.weather
    )
}
