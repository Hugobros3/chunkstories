package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.representation.Line
import xyz.chunkstories.api.graphics.systems.dispatching.LinesRenderer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration

private typealias VkLinesIR = MutableList<Line>

class VulkanLinesDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<Line, VkLinesIR>(backend) {

    override val representationName: String
        get() = Line::class.java.canonicalName

    inner class Drawer(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer<VkLinesIR>.() -> Unit) : VulkanDispatchingSystem.Drawer<VkLinesIR>(pass), LinesRenderer {
        override val system: VulkanDispatchingSystem<*, *>
            get() = this@VulkanLinesDispatcher

        init {
            drawerInitCode()
        }

        val vertexInput = vertexInputConfiguration {
            attribute {
                binding(0)
                format(VK10.VK_FORMAT_R32G32B32_SFLOAT)
                location(program.vertexInputs.find { it.name == "vertexIn" }!!.location)
                offset(0)
            }
            attribute {
                binding(0)
                format(VK10.VK_FORMAT_R32G32B32A32_SFLOAT)
                location(program.vertexInputs.find { it.name == "colorIn" }!!.location)
                offset(12)
            }

            binding {
                binding(0)
                stride(4 * 3 + 4 * 4)
                inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX)
            }
        }

        private val program = backend.shaderFactory.createProgram("colored", ShaderCompilationParameters(outputs = pass.declaration.outputs))
        private val pipeline = Pipeline(backend, program, pass, vertexInput, Primitive.LINES, FaceCullingMode.DISABLED)

        override fun registerDrawingCommands(context: VulkanPassInstance, commandBuffer: VkCommandBuffer, work: VkLinesIR) {
            val buffer = memAlloc(1024 * 1024) // 1Mb buffer
            var points = 0
            for (line in work) {
                buffer.putFloat(line.start.x.toFloat())
                buffer.putFloat(line.start.y.toFloat())
                buffer.putFloat(line.start.z.toFloat())

                buffer.putFloat(line.color.x)
                buffer.putFloat(line.color.y)
                buffer.putFloat(line.color.z)
                buffer.putFloat(line.color.w)

                buffer.putFloat(line.end.x.toFloat())
                buffer.putFloat(line.end.y.toFloat())
                buffer.putFloat(line.end.z.toFloat())

                buffer.putFloat(line.color.x)
                buffer.putFloat(line.color.y)
                buffer.putFloat(line.color.z)
                buffer.putFloat(line.color.w)

                points += 2
            }
            buffer.flip()
            val vkBuffer = VulkanVertexBuffer(backend, buffer.limit().toLong(), MemoryUsagePattern.DYNAMIC)
            vkBuffer.upload(buffer)
            memFree(buffer)

            if (points == 0)
                return

            val bindingContext = context.getBindingContext(pipeline)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

            vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vkBuffer.handle), MemoryStack.stackLongs(0))
            bindingContext.commitAndBind(commandBuffer)
            //print(points)
            vkCmdDraw(commandBuffer, points, 1, 0, 0)

            context.frame.recyclingTasks.add {
                vkBuffer.cleanup()
                bindingContext.recycle()
            }
        }

        override fun cleanup() {
            pipeline.cleanup()
            program.cleanup()
        }

    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer<VkLinesIR>.() -> Unit) = Drawer(pass, drawerInitCode)

    override fun sort(representations: Sequence<Line>, drawers: List<VulkanDispatchingSystem.Drawer<VkLinesIR>>, workForDrawers: MutableMap<VulkanDispatchingSystem.Drawer<VkLinesIR>, VkLinesIR>) {
        val lists = drawers.associateWith { mutableListOf<Line>() }

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
    }

    override fun cleanup() {

    }
}