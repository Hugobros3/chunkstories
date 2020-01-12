package xyz.chunkstories.graphics.vulkan.systems.world.farterrain

import org.joml.Vector2i
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.ImageInput
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.world.FarTerrainCellHelper
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.shaders.bindShaderResources
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.util.VkBuffer
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration
import xyz.chunkstories.world.WorldClientCommon

class VulkanFarTerrainRenderer(pass: VulkanPass, dslCode: VulkanFarTerrainRenderer.() -> Unit) : FarTerrainDrawer, VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend
    val client: IngameClient
        get() = backend.window.client.ingame!!

    val maxPatches = 4096
    val drawBufferSize: Long = 4L * 4 * maxPatches
    private val vkBuffers = InflightFrameResource<VulkanBuffer>(backend) {
        VulkanBuffer(backend, drawBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
    }
    private val uploadBuffer = memAlloc(drawBufferSize.toInt())

    private val program: VulkanShaderProgram
    private val pipeline: Pipeline

    private val helper = FarTerrainCellHelper(client.world)

    private val textureManager = FarTerrainTextureManager(backend, 0, 0, 4096 / 256)
    val sampler = VulkanSampler(backend, tilingMode = TextureTilingMode.CLAMP_TO_EDGE, scalingMode = ImageInput.ScalingMode.NEAREST)

    val indexesBuffer: VulkanBuffer

    init {
        dslCode()

        val shaderName = "farterrain"

        program = backend.shaderFactory.createProgram(shaderName)
        pipeline = Pipeline(backend, program, pass, vertexInputConfiguration { /** nothing hahahaha */ }, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

        val bb = memAlloc(16 * 1024 * 1024 * 2)
        bb.limit(bb.capacity())
        bb.position(0)
        indexesBuffer = VulkanBuffer(backend, bb, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, MemoryUsagePattern.STATIC)
    }

    override fun registerDrawingCommands(frame: VulkanFrame, ctx: SystemExecutionContext, commandBuffer: VkCommandBuffer) {
        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        ctx.bindShaderResources(bindingContext)

        val vkBuffer = vkBuffers[frame]
        bindingContext.bindTextureAndSampler("heightTexture", textureManager.heightTexture, sampler, 0)
        bindingContext.bindTextureAndSampler("terrainColor", textureManager.terrainColor, sampler, 0)
        bindingContext.bindSSBO("elementsBuffer", vkBuffer)
        bindingContext.preDraw(commandBuffer)

        val playerEntity = client.player.controlledEntity
        if(playerEntity != null) {
            val horizontalCoords = playerEntity.location.let { Vector2i(it.x.toInt(), it.z.toInt()) }

            if(helper.update(horizontalCoords)) {
                textureManager.requestUpdate(helper.currentSnappedCameraPos.x / 256, helper.currentSnappedCameraPos.y / 256, client.world as WorldClientCommon)
            }
        }
        //?.let {  }?.let { helper.update(it) }

        uploadBuffer.clear()
        val cells = helper.drawGrid(5)
        var i = 0
        val patchSize = 34
        while(!cells.isEmpty && i < maxPatches) {
            val size = cells.removeLast()
            val rsize = helper.sizes[size]
            val oz = cells.removeLast()
            val ox = cells.removeLast()

            uploadBuffer.putFloat(ox * 1.0f - (1f / 32f))
            uploadBuffer.putFloat(oz * 1.0f - (1f / 32f))
            uploadBuffer.putFloat(rsize * 1.0f / 32.0f)
            uploadBuffer.putInt(patchSize)
            //println("$ox $oz $rsize")
            //vkCmdDraw(commandBuffer, 2 * 3 * patchSize * patchSize, 1, 0, i)
            i++
        }
        vkCmdDraw(commandBuffer, 2 * 3 * patchSize * patchSize, i, 0, 0)
        //vkCmdBindIndexBuffer(commandBuffer, indexesBuffer.handle, 0, VK_INDEX_TYPE_UINT16)
        //vkCmdDrawIndexed(commandBuffer, 2 * 3 * patchSize * patchSize * i, 1, 0, 0, 0)
        //println(i)
        /*val patchSize = 32*/
        uploadBuffer.flip()

        vkBuffer.upload(uploadBuffer)

        //vkCmdDraw(commandBuffer, 2 * 3 * patchSize * patchSize, 1, 0, 0)

        frame.recyclingTasks.add {
            bindingContext.recycle()
        }
    }

    override fun cleanup() {
        pipeline.cleanup()
        program.cleanup()

        sampler.cleanup()

        memFree(uploadBuffer)
    }
}