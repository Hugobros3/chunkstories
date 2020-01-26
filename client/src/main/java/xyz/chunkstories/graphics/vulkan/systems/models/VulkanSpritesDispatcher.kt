package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Sprite
import xyz.chunkstories.api.graphics.systems.dispatching.SpritesRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.representations.RepresentationsGathered
import xyz.chunkstories.graphics.common.shaders.GLSLInstancedInput
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderTaskInstance
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.VulkanShaderResourcesContext
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler

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

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer(pass), SpritesRenderer {
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

            stackPush().use {
                val byteBuffer = stackMalloc(vertices.size * 4)
                vertices.forEach { f -> byteBuffer.putFloat(f) }
                byteBuffer.flip()

                vertexBuffer.upload(byteBuffer)
            }
        }

        val ssboBufferSize = 1024 * 1024L

        fun registerDrawingCommands(context: VulkanPassInstance, commandBuffer: VkCommandBuffer, work: WorkForDrawerInstance) {
            stackPush()

            val bindingContexts = mutableListOf<VulkanShaderResourcesContext>()

            val instancesGpuBuffer = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
            val instancesBuffer = MemoryUtil.memAlloc(instancesGpuBuffer.bufferSize.toInt())
            var instance = 0

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))

            for ((material, sprites) in work.queuedWork) {
                val bindingContext = context.getBindingContext(pipeline)
                bindingContexts.add(bindingContext)

                for (materialImageSlot in pipeline.program.glslProgram.materialImages) {
                    val textureName = material.textures[materialImageSlot.name] ?: "textures/notex.png"
                    bindingContext.bindTextureAndSampler(materialImageSlot.name, backend.textures.getOrLoadTexture2D(textureName), sampler)
                }
                bindingContext.bindInstancedInput(spriteII, instancesGpuBuffer)
                bindingContext.commitAndBind(commandBuffer)

                vkCmdDraw(commandBuffer, 3 * 2, sprites.size, 0, instance)
                for(sprite in sprites) {
                    //vkCmdDraw(commandBuffer, 3 * 2, 1, 0, instance)
                    extractInterfaceBlock(instancesBuffer, instance * instanceDataPaddedSize, sprite, spriteII.struct)
                    instance++
                }
            }

            instancesBuffer.position(instance * instanceDataPaddedSize)
            instancesBuffer.flip()

            instancesGpuBuffer.upload(instancesBuffer)

            memFree(instancesBuffer)

            context.frame.recyclingTasks.add {
                bindingContexts.forEach { it.recycle() }
                instancesGpuBuffer.cleanup()
            }

            stackPop()
        }

        override fun cleanup() {
            pipeline.cleanup()
            program.cleanup()
            vertexBuffer.cleanup()
        }
    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer.() -> Unit) = Drawer(pass, drawerInitCode)

    /*override fun sort(representations: Sequence<Sprite>, drawers: List<VulkanDispatchingSystem.Drawer<VkSpriteIR>>, workForDrawers: MutableMap<VulkanDispatchingSystem.Drawer<VkSpriteIR>, VkSpriteIR>) {
        val lists = drawers.associateWith { mutableListOf<Sprite>() }

        for (representation in representations) {
            for ((index, drawer) in drawers.withIndex()) {
                if ((drawer as VulkanSpritesDispatcher.Drawer).materialTag == representation.material.tag) {
                    lists[drawer]!!.add(representation)
                }
            }
        }

        for (entry in lists) {
            if (entry.value.isNotEmpty()) {
                workForDrawers[entry.key] = entry.value
            }
        }
    }*/

    class WorkForDrawerInstance(val drawerInstance: Pair<VulkanPassInstance, Drawer>) {
        val queuedWork = mutableMapOf<MeshMaterial, MutableList<Sprite>>()

        lateinit var cmdBuffer: VkCommandBuffer
    }

    override fun sortAndDraw(frame: VulkanFrame, drawers: Map<VulkanRenderTaskInstance, List<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>>>, maskedBuckets: Map<Int, RepresentationsGathered.Bucket>): Map<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>, VkCommandBuffer> {
        val allDrawersPlusInstances = drawers.values.flatten().filterIsInstance<Pair<VulkanPassInstance, Drawer>>()

        var workForDrawers = allDrawersPlusInstances.associateWith {
            WorkForDrawerInstance(it)
        }

        for ((mask, bucket) in maskedBuckets) {
            @Suppress("UNCHECKED_CAST") val somewhatRelevantDrawers = drawers.filter { it.key.mask and mask != 0 }.flatMap { it.value } as List<Pair<VulkanPassInstance, Drawer>>
            @Suppress("UNCHECKED_CAST") val representations = bucket.representations as ArrayList<Sprite>

            val drawerRelevancyMap = mutableMapOf<MeshMaterial, List<MutableList<Sprite>>>()

            for (sprite in representations) {
                val relevantWorkQueues = drawerRelevancyMap.getOrPut(sprite.material) {
                    somewhatRelevantDrawers.filter { it.second.materialTag == sprite.material.tag }.map {
                        workForDrawers[it]!!.queuedWork.getOrPut(sprite.material) {
                            arrayListOf()
                        }
                    }
                }

                for (queue in relevantWorkQueues) {
                    queue.add(sprite)
                }
            }
        }

        // Get rid of the work for the drawers that won't do anything
        workForDrawers = workForDrawers.filter {
            it.value.queuedWork.isNotEmpty()
        }

        for (workForDrawer in workForDrawers.values) {
            val cmdBuf = backend.renderGraph.commandPool.loanSecondaryCommandBuffer()
            workForDrawer.cmdBuffer = cmdBuf

            stackPush().use {
                val inheritInfo = VkCommandBufferInheritanceInfo.callocStack().apply {
                    sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
                    renderPass(workForDrawer.drawerInstance.first.pass.canonicalRenderPass.handle)
                    subpass(0)
                    framebuffer(VK_NULL_HANDLE
                            /** I don't know, I mean I could but I can't be arsed :P */)
                }
                val beginInfo = VkCommandBufferBeginInfo.callocStack().apply {
                    sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT or VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
                    pInheritanceInfo(inheritInfo)
                }
                vkBeginCommandBuffer(cmdBuf, beginInfo)
                workForDrawer.drawerInstance.first.pass.setScissorAndViewport(cmdBuf, workForDrawer.drawerInstance.first.renderTargetSize)
                workForDrawer.drawerInstance.second.registerDrawingCommands(workForDrawer.drawerInstance.first, cmdBuf, workForDrawer)
                vkEndCommandBuffer(cmdBuf)
            }
        }

        frame.recyclingTasks += {
            workForDrawers.forEach {
                backend.renderGraph.commandPool.returnSecondaryCommandBuffer(it.value.cmdBuffer)
            }
        }

        return workForDrawers.map { Pair(it.key, it.value.cmdBuffer) }.toMap()
    }

    override fun cleanup() {
        sampler.cleanup()
    }
}