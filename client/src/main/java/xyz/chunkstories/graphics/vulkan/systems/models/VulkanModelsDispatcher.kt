package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.Mesh
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.shaders.compiler.AvailableVertexInput
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.util.getVulkanFormat
import xyz.chunkstories.world.WorldClientCommon

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

    data class SpecializedPipelineKey(val shader: String, val enableAnimations: Boolean, val inputs: List<AvailableVertexInput>)

    fun getSpecializedPipelineKeyForMeshAndShader(mesh: Mesh, shader: String, shaderSupportsAnimations: Boolean, meshInstanceHasAnimationData: Boolean): SpecializedPipelineKey {
        val inputs = mesh.attributes.map { AvailableVertexInput(it.name, it.components, it.format) }
        val meshHasAnimationData = mesh.attributes.find { it.name == "boneIdIn" } != null

        return SpecializedPipelineKey(shader, meshHasAnimationData && shaderSupportsAnimations && meshInstanceHasAnimationData, inputs)
    }

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer<MeshInstance>(pass), ModelsRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String
        override var supportsAnimations: Boolean = false

        override val system: VulkanDispatchingSystem<ModelInstance>
            get() = this@VulkanModelsDispatcher

        init {
            this.apply(initCode)
        }

        inner class SpecializedPipeline(val key: SpecializedPipelineKey) : Cleanable {
            val program: VulkanShaderProgram
            val pipeline: Pipeline

            val compatibleInputs: List<AvailableVertexInput>
            val compatibleInputsIndexes: List<Int>

            init {
                val defines = mutableMapOf<String, String>()
                if (key.enableAnimations)
                    defines["ENABLE_ANIMATIONS"] = "true"

                program = backend.shaderFactory.createProgram(key.shader, ShaderCompilationParameters(outputs = pass.declaration.outputs, inputs = key.inputs, defines = defines))

                compatibleInputs = key.inputs.mapNotNull { input ->
                    if (program.glslProgram.vertexInputs.find { it.name == input.name } == null)
                        return@mapNotNull null
                    else
                        input
                }

                compatibleInputsIndexes = compatibleInputs.map { key.inputs.indexOf(it) }

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

        val ssboBufferSize = 1024 * 1024L

        override fun registerDrawingCommands(frame: VulkanFrame, ctx: SystemExecutionContext, commandBuffer: VkCommandBuffer, work: Sequence<MeshInstance>) {
            MemoryStack.stackPush()

            val client = backend.window.client.ingame ?: return

            val buckets = mutableMapOf<SpecializedPipelineKey, ArrayList<MeshInstance>>()
            for (meshInstance in work) {
                val key = getSpecializedPipelineKeyForMeshAndShader(meshInstance.mesh, shader, supportsAnimations, meshInstance.modelInstance.animator != null) //TODO cache keys per meshes
                val bucket = buckets.getOrPut(key) { arrayListOf() }

                bucket.add(meshInstance)
            }

            val realWorldTimeTruncated = (System.nanoTime() % 1000_000_000_000)
            val realWorldTimeMs = realWorldTimeTruncated / 1000_000
            val animationTime = (realWorldTimeMs / 1000.0) * 1000.0

            val bindingContexts = mutableListOf<DescriptorSetsMegapool.ShaderBindingContext>()

            for ((specializedPipelineKey, meshInstances) in buckets) {
                val specializedPipeline = specializedPipelines.getOrPut(specializedPipelineKey) { SpecializedPipeline(specializedPipelineKey) }

                val pipeline = specializedPipeline.pipeline

                val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
                bindingContexts += bindingContext

                val camera = ctx.passInstance.taskInstance.camera
                val world = client.world as WorldClientCommon

                bindingContext.bindUBO("camera", camera)
                bindingContext.bindUBO("world", world.getConditions())

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

                //TODO pool those
                val instancePositionSSBO = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
                val instancePositionsBuffer = MemoryUtil.memAlloc(instancePositionSSBO.bufferSize.toInt())
                var instance = 0

                bindingContext.preDraw(commandBuffer)

                val modelPositionII = specializedPipeline.program.glslProgram.instancedInputs.find { it.name == "modelPosition" }!!
                val modelPositionPaddedSize = getStd140AlignedSizeForStruct(modelPositionII.struct)

                for ((mesh, material, modelInstance) in meshInstances) {
                    val model: Model = modelInstance.model
                    val modelOnGpu = getGpuModelData(model)

                    val meshIndex = model.meshes.indexOf(mesh)
                    val meshOnGpu = modelOnGpu.meshesData[meshIndex]

                    for (inputIndex in specializedPipeline.compatibleInputsIndexes) {
                        val vertexBuffer = meshOnGpu.attributesVertexBuffers[inputIndex]
                        vkCmdBindVertexBuffers(commandBuffer, inputIndex, stackLongs(vertexBuffer.handle), stackLongs(0))
                    }

                    extractInterfaceBlock(instancePositionsBuffer, instance * modelPositionPaddedSize, modelInstance.position, modelPositionII.struct)

                    val perMeshBindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
                    bindingContexts += perMeshBindingContext

                    for (materialImageSlot in pipeline.program.glslProgram.materialImages) {
                        val textureName = material.textures[materialImageSlot.name] ?: "textures/notex.png"
                        perMeshBindingContext.bindTextureAndSampler(materialImageSlot.name, backend.textures.getOrLoadTexture2D(textureName), sampler)
                        //println(pipeline.program.glslProgram)
                    }

                    if (specializedPipeline.key.enableAnimations) {
                        val bonez = ExperimentalBonesData()
                        val animator = modelInstance.animator!!
                        for ((boneName, i) in mesh.boneIds!!) {
                            bonez.bones[i].set(animator.getBoneHierarchyTransformationMatrixWithOffset(boneName, animationTime))
                        }

                        perMeshBindingContext.bindUBO("animationData", bonez)
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