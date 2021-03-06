package xyz.chunkstories.graphics.vulkan.systems.dispatching.lights

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.representation.PointLight
import xyz.chunkstories.api.graphics.systems.dispatching.DefferedLightsRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.representations.RepresentationsGathered
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderTaskInstance
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.VulkanShaderResourcesContext
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.dispatching.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration

class VulkanDefferedLightsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<PointLight>(backend) {

    override val representationName: String
        get() = PointLight::class.java.canonicalName

    inner class Drawer(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer(pass), DefferedLightsRenderer {
        override val system: VulkanDispatchingSystem<*>
            get() = this@VulkanDefferedLightsDispatcher

        private val vertexBuffer: VulkanVertexBuffer

        init {
            drawerInitCode()

            val vertices = floatArrayOf(-1.0F, -3.0F, 3.0F, 1.0F, -1.0F, 1.0F)
            vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L, MemoryUsagePattern.STATIC)

            MemoryStack.stackPush().use {
                val byteBuffer = MemoryStack.stackMalloc(vertices.size * 4)
                vertices.forEach { f -> byteBuffer.putFloat(f) }
                byteBuffer.flip()

                vertexBuffer.upload(byteBuffer)
            }
        }

        val vertexInputConfiguration = vertexInputConfiguration {
            binding {
                binding(0)
                stride(2 * 4)
                inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
            }

            attribute {
                binding(0)
                location(program.vertexInputs.find { it.name == "vertexIn" }?.location!!)
                format(VK_FORMAT_R32G32_SFLOAT)
                offset(0)
            }
        }

        private val program = backend.shaderFactory.createProgram("pointLight", ShaderCompilationParameters(outputs = pass.declaration.outputs))
        private val pipeline = Pipeline(backend, program, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.DISABLED)

        override fun registerDrawingCommands(drawerWork: DrawerWork) {
            val work = drawerWork as DefferedLightsDrawerWork
            val context = work.drawerInstance.first
            val commandBuffer = work.cmdBuffer

            val bindingContexts = mutableListOf<VulkanShaderResourcesContext>()

            for (light in work.pointLights) {
                val bindingContext = context.getBindingContext(pipeline)
                bindingContexts.add(bindingContext)
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
                bindingContext.bindStructuredUBO("light", light)

                vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
                bindingContext.commitAndBind(commandBuffer)

                vkCmdDraw(commandBuffer, 3, 1, 0, 0)
            }

            context.frame.recyclingTasks.add {
                bindingContexts.forEach { it.recycle() }
            }
        }

        override fun cleanup() {
            pipeline.cleanup()
            program.cleanup()

            vertexBuffer.cleanup()
        }

    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer.() -> Unit) = Drawer(pass, drawerInitCode)

    /*fun sort(representations: Sequence<PointLight>, drawers: List<VulkanDispatchingSystem.Drawer<VkDefferedLightIR>>, workForDrawers: MutableMap<VulkanDispatchingSystem.Drawer<VkDefferedLightIR>, VkDefferedLightIR>) {
        val lists = drawers.associateWith { mutableListOf<PointLight>() }

        for (representation in representations) {
            for (drawer in drawers) {
                lists[drawer]!!.add(representation)
            }
        }

        for (entry in lists) {
            if (entry.value.isNotEmpty()) {
                workForDrawers[entry.key] = entry.value
            }
        }
    }*/
    class DefferedLightsDrawerWork(drawerInstance: Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>) : DrawerWork(drawerInstance) {
        val pointLights = arrayListOf<PointLight>()
        override fun isEmpty(): Boolean = pointLights.isEmpty()
    }

    override fun sortWork(frame: VulkanFrame, drawers: Map<VulkanRenderTaskInstance, List<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>>>, maskedBuckets: Map<Int, RepresentationsGathered.Bucket>): Map<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>, DrawerWork> {
        val allDrawersPlusInstances = drawers.values.flatten().filterIsInstance<Pair<VulkanPassInstance, Drawer>>()

        val workForDrawers = allDrawersPlusInstances.associateWith {
            DefferedLightsDrawerWork(it)
        }

        for ((mask, bucket) in maskedBuckets) {
            @Suppress("UNCHECKED_CAST") val somewhatRelevantDrawers = drawers.filter { it.key.mask and mask != 0 }.flatMap { it.value } as List<Pair<VulkanPassInstance, Drawer>>
            @Suppress("UNCHECKED_CAST") val representations = bucket.representations as ArrayList<PointLight>

            for (light in representations) {
                for (e in somewhatRelevantDrawers) {
                    val queue = workForDrawers[e]!!
                    queue.pointLights.add(light)
                }
            }
        }

        return workForDrawers.map { Pair(it.key, it.value) }.toMap()
    }

    override fun cleanup() {

    }
}