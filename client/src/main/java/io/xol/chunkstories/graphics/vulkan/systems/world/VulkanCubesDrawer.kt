package io.xol.chunkstories.graphics.vulkan.systems.world

import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.graphics.structs.Camera
import io.xol.chunkstories.api.graphics.structs.WorldConditions
import io.xol.chunkstories.api.physics.Frustrum
import io.xol.chunkstories.api.util.kotlin.toVec3i
import io.xol.chunkstories.api.world.World
import io.xol.chunkstories.client.InternalClientOptions
import io.xol.chunkstories.graphics.common.FaceCullingMode
import io.xol.chunkstories.graphics.common.Primitive
import io.xol.chunkstories.graphics.common.world.ChunkRenderInfo
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VertexInputConfiguration
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import io.xol.chunkstories.world.WorldClientCommon
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer

class VulkanCubesDrawer(pass: VulkanPass, val client: IngameClient) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    //private val descriptorPool = DescriptorPool(backend, pass.program)
    private val vertexInputConfiguration = VertexInputConfiguration {
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

    private val pipeline = Pipeline(backend, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)


    companion object {
        var totalCubesDrawn = 0
        var totalBuffersUsed = 0
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        MemoryStack.stackPush()

        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)

        val entity = client.player.controlledEntity
        val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val world = client.world as WorldClientCommon

        //descriptorPool.configure(frame, camera)
        bindingContext.bindUBO(camera)
        bindingContext.bindUBO(world.getConditions())
        bindingContext.preDraw(commandBuffer)

        val frustrum = Frustrum(camera, client.gameWindow)

        totalCubesDrawn = 0
        totalBuffersUsed = 0

        //vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 1, descriptorPool.setsForFrame(frame), null as? IntArray)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

        val camChunk = camera.position.toVec3i()
        camChunk.x /= 32
        camChunk.y /= 32
        camChunk.z /= 32

        val drawDistance = world.client.configuration.getIntValue(InternalClientOptions.viewDistance) / 32
        val drawDistanceH = 4

        val usedData = mutableListOf<ChunkVkMeshProperty.ChunkVulkanMeshData>()

        //var box = Box(Vector3d(0.0), Vector3d(0.0))
        //box.xWidth = 32.0
        //box.yHeight = 32.0
        //box.zWidth = 32.0

        val boxCenter = Vector3f(0f)
        val boxSize = Vector3f(32f, 32f, 32f)

        val sortedChunks = world.allLoadedChunks.filter { chunk ->
            boxCenter.x = chunk.chunkX * 32.0f + 16.0f
            boxCenter.y = chunk.chunkY * 32.0f + 16.0f
            boxCenter.z = chunk.chunkZ * 32.0f + 16.0f

            frustrum.isBoxInFrustrum(boxCenter, boxSize) && !chunk.isAirChunk
        }.sortedBy { chunk ->
            val chunkCenter = Vector3f(chunk.chunkX * 32 + 16.0f, chunk.chunkY * 32 + 16.0f, chunk.chunkZ * 32 + 16.0f)
            chunkCenter.distance(camera.position) + 0.0f
        }.distinct()

        for (chunk in sortedChunks) {
            if (chunk.chunkX !in (camChunk.x - drawDistance)..(camChunk.x + drawDistance))
                continue

            if ((chunk.chunkZ !in (camChunk.z - drawDistance)..(camChunk.z + drawDistance)))
                continue

            if ((chunk.chunkY !in (camChunk.y - drawDistanceH)..(camChunk.y + drawDistanceH)))
                continue

            if (chunk.meshData is ChunkVkMeshProperty) {
                val block = (chunk.meshData as ChunkVkMeshProperty).get()
                if (block != null)
                    usedData.add(block)

                if (block?.vertexBuffer != null) {
                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 0, stackLongs(block.virtualTexturingContext!!.setHandle), null)
                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(block.vertexBuffer.handle), stackLongs(0))

                    if(block.perChunkBindings == null) {
                        val chunkRenderInfo = ChunkRenderInfo().apply {
                            chunkX = chunk.chunkX
                            chunkY = chunk.chunkY
                            chunkZ = chunk.chunkZ
                        }
                        block.perChunkBindings = backend.descriptorMegapool.getBindingContext(pipeline).also {
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

        frame.recyclingTasks.add {
            usedData.forEach(ChunkVkMeshProperty.ChunkVulkanMeshData::release)
            bindingContext.recycle()
        }

        MemoryStack.stackPop()
    }

    override fun cleanup() {
        /*for (chunk in client.world.allLoadedChunks) {
            (chunk.mesh() as? VulkanChunkRenderData)?.destroy()
        }*/

        //descriptorPool.cleanup()
        pipeline.cleanup()
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
