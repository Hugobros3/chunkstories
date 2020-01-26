package xyz.chunkstories.graphics.vulkan.systems.dispatching.lines

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.representation.Line
import xyz.chunkstories.api.graphics.systems.dispatching.LinesRenderer
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
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.dispatching.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration

class VulkanLinesDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<Line>(backend) {

    override val representationName: String
        get() = Line::class.java.canonicalName

    inner class Drawer(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer(pass), LinesRenderer {
        override val system: VulkanDispatchingSystem<*>
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

        override fun registerDrawingCommands(drawerWork: DrawerWork) {
            val work = drawerWork as LinesDrawerWork
            val context = work.drawerInstance.first
            val commandBuffer = work.cmdBuffer

            val buffer = memAlloc(1024 * 1024) // 1Mb buffer
            var points = 0
            for (line in work.lines) {
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

    class LinesDrawerWork(drawerInstance: Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>) : DrawerWork(drawerInstance) {
        val lines = arrayListOf<Line>()
        override fun isEmpty(): Boolean = lines.isEmpty()
    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer.() -> Unit) = Drawer(pass, drawerInitCode)

    override fun sortWork(frame: VulkanFrame, drawers: Map<VulkanRenderTaskInstance, List<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>>>, maskedBuckets: Map<Int, RepresentationsGathered.Bucket>): Map<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>, DrawerWork> {
        val allDrawersPlusInstances = drawers.values.flatten().filterIsInstance<Pair<VulkanPassInstance, Drawer>>()

        val workForDrawers = allDrawersPlusInstances.associateWith {
            LinesDrawerWork(it)
        }

        for ((mask, bucket) in maskedBuckets) {
            @Suppress("UNCHECKED_CAST") val somewhatRelevantDrawers = drawers.filter { it.key.mask and mask != 0 }.flatMap { it.value } as List<Pair<VulkanPassInstance, Drawer>>
            @Suppress("UNCHECKED_CAST") val representations = bucket.representations as ArrayList<Line>

            for (line in representations) {
                for (e in somewhatRelevantDrawers) {
                    val queue = workForDrawers[e]!!
                    queue.lines.add(line)
                }
            }
        }

        return workForDrawers.map { Pair(it.key, it.value) }.toMap()
    }

    override fun cleanup() {

    }
}