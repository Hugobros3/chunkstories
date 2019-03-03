package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.Mesh
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.shaders.GLSLInstancedInput
import xyz.chunkstories.graphics.common.shaders.compiler.AvailableVertexInput
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
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.util.getVulkanFormat
import xyz.chunkstories.world.WorldClientCommon
import java.nio.ByteBuffer

class VulkanModelsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<ModelInstance>(backend) {

    override val representationName: String = ModelInstance::class.java.canonicalName

    val gpuUploadedModels = mutableMapOf<Model, GpuModelData>()
    val sampler = VulkanSampler(backend)

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

    data class SpecializedPipelineKey(val shader: String, val inputs: List<AvailableVertexInput>)

    fun getSpecializedPipelineKeyForMeshAndShader(mesh: Mesh, shader: String): SpecializedPipelineKey {
        val inputs = mesh.attributes.map { AvailableVertexInput(it.name, it.components, it.format) }
        return SpecializedPipelineKey(shader, inputs)
    }

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer<MeshInstance>(pass), ModelsRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String

        override val system: VulkanDispatchingSystem<ModelInstance>
            get() = this@VulkanModelsDispatcher

        init {
            this.apply(initCode)
        }

        inner class SpecializedPipeline(val key: SpecializedPipelineKey) : Cleanable {
            val program = backend.shaderFactory.createProgram(key.shader, ShaderCompilationParameters(outputs = pass.declaration.outputs, inputs = key.inputs))
            val pipeline: Pipeline

            val canDoAnimation = false

            val compatibleInputs = key.inputs.mapNotNull { input ->
                if (program.glslProgram.vertexInputs.find { it.name == input.name } == null)
                    return@mapNotNull null
                else
                    input
            }

            val compatibleInputsIndexes = compatibleInputs.map { key.inputs.indexOf(it) }

            init {
                val vertexInputs = VertexInputConfiguration {
                    for ((i, input) in compatibleInputs.withIndex()) {
                        val j = i

                        //TODO this doesn't account for real-world alignment requirements :(
                        val vulkanFormat = getVulkanFormat(input.format, input.components)
                        val size = input.format.bytesPerComponent * input.components

                        attribute {
                            binding(j)
                            location(program.vertexInputs.find { it.name == input.name }!!.location)
                            format(vulkanFormat.ordinal)
                            offset(0)
                        }

                        binding {
                            binding(j)
                            stride(size)
                            inputRate()
                        }
                    }
                }

                pipeline = Pipeline(backend, program, pass, vertexInputs, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)
            }

            override fun cleanup() {
                program.cleanup()
                pipeline.cleanup()
            }
        }

        val specializedPipelines = mutableMapOf<SpecializedPipelineKey, SpecializedPipeline>()

        fun getAlignedsizeForStruct(instancedStruct: GLSLInstancedInput): Int {
            //val instancedStruct = glslProgram.instancedInputs.find { it.name == name } ?: throw Exception("No instanced input named: $name")
            val structSize = instancedStruct.struct.size
            val sizeAligned16 = if (structSize % 16 == 0) structSize else (structSize / 16 * 16) + 16
            return sizeAligned16
        }

        val ssboBufferSize = 1024 * 1024L

        override fun registerDrawingCommands(frame: Frame, context: VulkanFrameGraph.FrameGraphNode.PassNode, commandBuffer: VkCommandBuffer, work: Sequence<MeshInstance>) {
            val client = backend.window.client.ingame ?: return

            //Efficient scheduling:
            //Make Pair<Mesh, ModelInstance>
            //Sort these in buckets by specialized pipeline key, cache key per mesh because key is sort heavy to create
            //Foreach bucket: bind pipeline and render meshes

            val buckets = mutableMapOf<SpecializedPipelineKey, ArrayList<MeshInstance>>()

            /*for (instance in modelInstances) {
                for ((i, mesh) in instance.model.meshes.withIndex()) {
                    if (instance.meshesMask and (1 shl i) == 0)
                        continue

                    val paired = Pair(mesh, instance)
                    //all.add(paired)

                    val key = getSpecializedPipelineKeyForMeshAndShader(mesh, "models") //TODO mesh.material.shader & instance.materialOverride.shader
                    val bucket = buckets.getOrPut(key) { arrayListOf() }

                    bucket.add(paired)
                }
            }*/
            for (meshInstance in work) {
                val key = getSpecializedPipelineKeyForMeshAndShader(meshInstance.mesh, shader) //TODO mesh.material.shader & instance.materialOverride.shader
                val bucket = buckets.getOrPut(key) { arrayListOf() }

                bucket.add(meshInstance)
            }

            MemoryStack.stackPush()

            val bindingContexts = mutableListOf<DescriptorSetsMegapool.ShaderBindingContext>()

            for ((specializedPipelineKey, meshInstances) in buckets) {
                val specializedPipeline = specializedPipelines.getOrPut(specializedPipelineKey) { SpecializedPipeline(specializedPipelineKey) }

                val pipeline = specializedPipeline.pipeline

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

                bindingContext.preDraw(commandBuffer)

                val modelPositionII = specializedPipeline.program.glslProgram.instancedInputs.find { it.name == "modelPosition" }!!
                val modelPositionPaddedSize = getAlignedsizeForStruct(modelPositionII)

                for ((mesh, material, modelInstance) in meshInstances) {
                    val model: Model = modelInstance.model
                    val modelOnGpu = getGpuModelData(model)

                    val meshIndex = model.meshes.indexOf(mesh)

                    //for ((meshIndex, mesh) in model.meshes.withIndex()) {
                    val meshOnGpu = modelOnGpu.meshesData[meshIndex]

                    /*val vertexPosAttribute = mesh.attributes.find { it.name == "vertexPosition" }!!
                    val vertexPosAttributeIndex = mesh.attributes.indexOf(vertexPosAttribute)

                    vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(meshOnGpu.attributesVertexBuffers[vertexPosAttributeIndex].handle), stackLongs(0))*/

                    for (inputIndex in specializedPipeline.compatibleInputsIndexes) {
                        val vertexBuffer = meshOnGpu.attributesVertexBuffers[inputIndex]
                        vkCmdBindVertexBuffers(commandBuffer, inputIndex, stackLongs(vertexBuffer.handle), stackLongs(0))
                    }

                    fun writeInterfaceBlock(byteBuffer: ByteBuffer, offset: Int, interfaceBlock: InterfaceBlock, glslResource: GLSLInstancedInput) {
                        byteBuffer.position(offset)

                        for (field in glslResource.struct.fields) {
                            byteBuffer.position(offset + field.offset)
                            extractInterfaceBlockField(field, byteBuffer, interfaceBlock)
                        }
                    }

                    //instancePositionsBuffer.position(instance * modelPositionPaddedSize)
                    writeInterfaceBlock(instancePositionsBuffer, instance * modelPositionPaddedSize, modelInstance.position, modelPositionII)

                    val perMeshBindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
                    bindingContexts += perMeshBindingContext

                    val material = modelInstance.materials[meshIndex] ?: mesh.material
                    for (materialImageSlot in pipeline.program.glslProgram.materialImages) {
                        val textureName = material.textures[materialImageSlot.name] ?: "textures/notex.png"
                        perMeshBindingContext.bindTextureAndSampler(materialImageSlot.name, backend.textures.getOrLoadTexture2D(textureName), sampler)
                        //println(pipeline.program.glslProgram)
                    }


                    perMeshBindingContext.bindSSBO("modelPosition", instancePositionSSBO)
                    perMeshBindingContext.preDraw(commandBuffer)

                    vkCmdDraw(commandBuffer, mesh.vertices, 1, 0, instance)

                    instance++

                    frame.stats.totalVerticesDrawn += mesh.vertices
                    frame.stats.totalDrawcalls++
                    //}
                }

                instancePositionsBuffer.position(instance * modelPositionPaddedSize)
                instancePositionsBuffer.flip()

                instancePositionSSBO.upload(instancePositionsBuffer)

                MemoryUtil.memFree(instancePositionsBuffer)

                frame.recyclingTasks.add {
                    instancePositionSSBO.cleanup()
                }
            }

            frame.recyclingTasks.add {
                bindingContexts.forEach { it.recycle() }
            }

            MemoryStack.stackPop()
        }

        override fun cleanup() {
            specializedPipelines.values.forEach(Cleanable::cleanup)
        }
    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer<*>.() -> Unit) = Drawer(pass, drawerInitCode)

    /*override fun <T> sort(representation: ModelInstance, drawers: Array<VulkanDispatchingSystem.Drawer<T>>, outputs: List<MutableList<T>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }*/

    data class MeshInstance(val mesh: Mesh, val material: MeshMaterial, val modelInstance: ModelInstance)

    override fun sort(instance: ModelInstance, drawers: Array<VulkanDispatchingSystem.Drawer<*>>, outputs: List<MutableList<Any>>) {
        for ((i, mesh) in instance.model.meshes.withIndex()) {
            if (instance.meshesMask and (1 shl i) == 0)
                continue

            val meshInstance = MeshInstance(mesh, instance.materials[i] ?: mesh.material, instance)

            for ((index, drawer) in drawers.withIndex()) {
                if ((drawer as VulkanModelsDispatcher.Drawer).materialTag == meshInstance.material.tag) {
                    outputs[index].add(meshInstance)
                }
            }
        }
    }

    override fun cleanup() {
        gpuUploadedModels.values.forEach(Cleanable::cleanup)
        gpuUploadedModels.clear()

        sampler.cleanup()
    }
}