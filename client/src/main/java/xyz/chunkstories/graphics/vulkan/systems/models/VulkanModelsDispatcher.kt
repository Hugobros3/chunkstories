package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.Mesh
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.buffers.extractInterfaceBlockField
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.systems.world.getConditions
import xyz.chunkstories.world.WorldClientCommon

class VulkanModelsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<ModelInstance>(backend) {
    override val representationName: String = ModelInstance::class.java.canonicalName

    val gpuUploadedModels = mutableMapOf<Model, GpuModelData>()

    inner class GpuModelData(val model: Model) : Cleanable {
        val meshesData = model.meshes.map { GpuMeshesData(it) }

        inner class GpuMeshesData(val mesh: Mesh) {
            val attributesVertexBuffers = mesh.attributes.map {
                val data = it.data
                val vb = VulkanVertexBuffer(backend, data.capacity().toLong(), MemoryUsagePattern.SEMI_STATIC)

                data.position(0)
                data.limit(data.capacity())
                vb.upload(it.data)
                vb
            }
        }

        override fun cleanup() {
            meshesData.forEach {
                it.attributesVertexBuffers.forEach {
                    it.cleanup()
                }
            }
        }
    }

    fun getGpuModelData(model: Model) =
        gpuUploadedModels.getOrPut(model) { GpuModelData(model) }

    private val meshesVertexInputCfg = VertexInputConfiguration {
        var offset = 0

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }!!.location)
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(offset)
        }
        offset += 4 * 3

        binding {
            binding(0)
            stride(offset)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }
    }

    inner class Drawer(pass: VulkanPass) : VulkanDispatchingSystem.Drawer<ModelInstance>(pass) {
        override val system: VulkanDispatchingSystem<ModelInstance>
            get() = this@VulkanModelsDispatcher


        private val program = backend.shaderFactory.createProgram("models", ShaderCompilationParameters(outputs = pass.declaration.outputs))
        //TODO create pipelines on the fly to accommodate models attributes
        private val pipeline = Pipeline(backend, program, pass, meshesVertexInputCfg, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

        val instancedStruct = program.glslProgram.instancedInputs.find { it.name == "modelPosition" }!!
        val structSize = instancedStruct.struct.size
        val sizeAligned16 = if (structSize % 16 == 0) structSize else (structSize / 16 * 16) + 16

        val ssboBufferSize = 1024 * 1024L

        override fun registerDrawingCommands(frame: Frame, context: VulkanFrameGraph.FrameGraphNode.PassNode, commandBuffer: VkCommandBuffer, modelInstances: Sequence<ModelInstance>) {
            val client = backend.window.client.ingame ?: return

            MemoryStack.stackPush()

            val bindingContexts = mutableListOf<DescriptorSetsMegapool.ShaderBindingContext>()
            val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
            bindingContexts += bindingContext

            val camera = context.context.camera
            val world = client.world as WorldClientCommon

            bindingContext.bindUBO("camera", camera)
            bindingContext.bindUBO("world", world.getConditions())

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

            //if (backend.logicalDevice.enableMagicTexturing)
            //    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipelineLayout, 0, MemoryStack.stackLongs(backend.textures.magicTexturing!!.theSet), null)

            //TODO pool those
            val instancePositionSSBO = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
            val instancePositionsBuffer = MemoryUtil.memAlloc(instancePositionSSBO.bufferSize.toInt())
            var instance = 0

            bindingContext.bindSSBO("modelPosition", instancePositionSSBO)

            bindingContext.preDraw(commandBuffer)

            for (modelInstance in modelInstances) {

                //println("dispatching: "+modelInstance)

                val model: Model = modelInstance.model
                val modelOnGpu = getGpuModelData(model)

                for((meshIndex, mesh) in model.meshes.withIndex()) {
                    val meshOnGpu = modelOnGpu.meshesData[meshIndex]

                    val vertexPosAttribute = mesh.attributes.find { it.name == "vertexPosition" }!!
                    val vertexPosAttributeIndex = mesh.attributes.indexOf(vertexPosAttribute)

                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(meshOnGpu.attributesVertexBuffers[vertexPosAttributeIndex].handle), stackLongs(0))

                    instancePositionsBuffer.position(instance * sizeAligned16)

                    for (field in instancedStruct.struct.fields) {
                        instancePositionsBuffer.position(instance * sizeAligned16 + field.offset)
                        extractInterfaceBlockField(field, instancePositionsBuffer, modelInstance.position)
                    }

                    vkCmdDraw(commandBuffer, mesh.vertices, 1, 0, instance)

                    instance++

                    frame.stats.totalVerticesDrawn += mesh.vertices
                    frame.stats.totalDrawcalls++
                }
            }

            instancePositionsBuffer.position(instance * sizeAligned16)
            instancePositionsBuffer.flip()

            instancePositionSSBO.upload(instancePositionsBuffer)

            MemoryUtil.memFree(instancePositionsBuffer)

            frame.recyclingTasks.add {
                bindingContext.recycle()
                instancePositionSSBO.cleanup()//TODO recycle don't destroy!
            }

            MemoryStack.stackPop()
        }

        override fun cleanup() {
            pipeline.cleanup()
            program.cleanup()
        }
    }

    override fun createDrawerForPass(pass: VulkanPass) = Drawer(pass)

    override fun cleanup() {
        gpuUploadedModels.values.forEach(Cleanable::cleanup)
        gpuUploadedModels.clear()
    }
}