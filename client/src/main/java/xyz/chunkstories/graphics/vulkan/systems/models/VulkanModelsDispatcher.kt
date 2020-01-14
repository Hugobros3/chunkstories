package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.Mesh
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.getConditions
import xyz.chunkstories.graphics.common.shaders.compiler.AvailableVertexInput
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.structs.SkeletalAnimationData
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.common.util.getStd140AlignedSizeForStruct
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VertexInputConfiguration
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.util.getVulkanFormat
import java.util.concurrent.ConcurrentHashMap

class VulkanModelsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<ModelInstance, VkModelsIR>(backend) {
    override val representationName: String = ModelInstance::class.java.canonicalName
    val sampler = VulkanSampler(backend)

    /*val gpuUploadedModels = mutableMapOf<Model, GpuModelData>()

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
            gpuUploadedModels.getOrPut(model) { GpuModelData(model) }*/

    val gpuUploadedMeshes = ConcurrentHashMap<Mesh, GpuMeshData>()

    inner class GpuMeshData(val mesh: Mesh): Cleanable {
        val attributesVertexBuffers = mesh.attributes.map {
            val data = it.data
            val vb = VulkanVertexBuffer(backend, data.capacity().toLong(), MemoryUsagePattern.SEMI_STATIC)

            data.position(0)
            data.limit(data.capacity())
            vb.upload(it.data)
            vb
        }

        override fun cleanup() {
            attributesVertexBuffers.forEach(Cleanable::cleanup)
        }
    }

    fun getGpuMeshData(mesh: Mesh) =
            gpuUploadedMeshes.getOrPut(mesh) { GpuMeshData(mesh) }

    data class SpecializedPipelineKey(val shader: String, val enableAnimations: Boolean, val inputs: List<AvailableVertexInput>)

    fun getSpecializedPipelineKeyForMeshAndShader(mesh: Mesh, shader: String, shaderSupportsAnimations: Boolean, meshInstanceHasAnimationData: Boolean): SpecializedPipelineKey {
        val inputs = mesh.attributes.map { AvailableVertexInput(it.name, it.components, it.format) }
        val meshHasAnimationData = mesh.attributes.find { it.name == "boneIdIn" } != null

        return SpecializedPipelineKey(shader, meshHasAnimationData && shaderSupportsAnimations && meshInstanceHasAnimationData, inputs)
    }

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer<VkModelsIR>(pass), ModelsRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String
        override var supportsAnimations: Boolean = false

        override val system: VulkanDispatchingSystem<ModelInstance, VkModelsIR>
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

        private val ssboBufferSize = 1024 * 1024L

        override fun registerDrawingCommands(frame: VulkanFrame, context: SystemExecutionContext, commandBuffer: VkCommandBuffer, work: VkModelsIR) {
            MemoryStack.stackPush()

            val modelPositionsBuffer = MemoryUtil.memAlloc(ssboBufferSize.toInt())
            val animationDataBuffer = MemoryUtil.memAlloc(ssboBufferSize.toInt())

            val client = backend.window.client.ingame ?: return

            val realWorldTimeTruncated = (System.nanoTime() % 1000_000_000_000)
            val realWorldTimeMs = realWorldTimeTruncated / 1000_000
            val animationTime = (realWorldTimeMs / 1000.0) * 1000.0

            val bindingContexts = mutableListOf<DescriptorSetsMegapool.ShaderBindingContext>()

            for((mesh, nonAnimatedInstances, animatedInstances) in work) {
                fun handle(entries: Collection<Map.Entry<MeshMaterial, MeshMaterialInstances>>, animated: Boolean) {
                    if(entries.isEmpty())
                        return

                    // Obtain a specialized pipeline that works for this mesh and animation state
                    val specializedPipelineKey = getSpecializedPipelineKeyForMeshAndShader(mesh, shader, supportsAnimations, animated)
                    val specializedPipeline = specializedPipelines.getOrPut(specializedPipelineKey) { SpecializedPipeline(specializedPipelineKey) }
                    val pipeline = specializedPipeline.pipeline
                    val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
                    bindingContexts += bindingContext

                    val camera = context.passInstance.taskInstance.camera
                    val world = client.world

                    bindingContext.bindStructuredUBO("camera", camera)
                    bindingContext.bindStructuredUBO("world", world.getConditions())

                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
                    bindingContext.commitAndBind(commandBuffer)

                    var instance = 0

                    // TODO pool those allocations
                    val modelPositionsGpuBuffer = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
                    //val modelPositionsBuffer = MemoryUtil.memAlloc(modelPositionsGpuBuffer.bufferSize.toInt())
                    modelPositionsBuffer.clear()

                    val modelPositionsInstancedInput = specializedPipeline.program.glslProgram.instancedInputs.find { it.name == "modelPosition" }!!
                    val modelPositionsPaddedSize = getStd140AlignedSizeForStruct(modelPositionsInstancedInput.struct)


                    val animationDataGpuBuffer = if(animated) VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC) else null
                    //val animationDataBuffer = if(animated) MemoryUtil.memAlloc(animationDataGpuBuffer!!.bufferSize.toInt()) else null
                    animationDataBuffer.clear()

                    val animationDataInstancedInput = if(animated) specializedPipeline.program.glslProgram.instancedInputs.find { it.name == "animationData" } else null
                    val animationDataPaddedSize = if(animationDataInstancedInput != null) getStd140AlignedSizeForStruct(animationDataInstancedInput.struct) else 0


                    // Get and bind mesh vertex data
                    val meshOnGpu = getGpuMeshData(mesh)
                    for (inputIndex in specializedPipeline.compatibleInputsIndexes) {
                        val vertexBuffer = meshOnGpu.attributesVertexBuffers[inputIndex]
                        vkCmdBindVertexBuffers(commandBuffer, inputIndex, stackLongs(vertexBuffer.handle), stackLongs(0))
                    }

                    for ((material, meshMaterialInstances) in entries) {
                        val perMaterialBindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
                        bindingContexts += perMaterialBindingContext

                        val firstInstance = instance
                        var materialInstancesCount = 0

                        for (materialImageSlot in pipeline.program.glslProgram.materialImages) {
                            val textureName = material.textures[materialImageSlot.name] ?: "textures/notex.png"
                            perMaterialBindingContext.bindTextureAndSampler(materialImageSlot.name, backend.textures.getOrLoadTexture2D(textureName), sampler)
                            //println(pipeline.program.glslProgram)
                        }

                        for(modelInstance in meshMaterialInstances.instances) {
                            extractInterfaceBlock(modelPositionsBuffer, instance * modelPositionsPaddedSize, modelInstance.position, modelPositionsInstancedInput.struct)

                            if(animated) {
                                val bonez = SkeletalAnimationData()
                                val animator = modelInstance.animator!!
                                for ((boneName, i) in mesh.boneIds!!) {
                                    bonez.bones[i].set(animator.getBoneHierarchyTransformationMatrixWithOffset(boneName, animationTime))
                                }
                                extractInterfaceBlock(animationDataBuffer, instance * animationDataPaddedSize, bonez, animationDataInstancedInput!!.struct)
                            }

                            instance++
                            materialInstancesCount++
                        }

                        perMaterialBindingContext.bindInstancedInput("modelPosition", modelPositionsGpuBuffer)
                        if(animated) {
                            perMaterialBindingContext.bindInstancedInput("animationData", animationDataGpuBuffer!!)
                        }
                        perMaterialBindingContext.commitAndBind(commandBuffer)


                        vkCmdDraw(commandBuffer, mesh.vertices, materialInstancesCount, 0, firstInstance)

                        frame.stats.totalVerticesDrawn += mesh.vertices
                        frame.stats.totalDrawcalls++
                    }

                    modelPositionsBuffer.position(instance * modelPositionsPaddedSize)
                    modelPositionsBuffer.flip()
                    modelPositionsGpuBuffer.upload(modelPositionsBuffer)

                    frame.recyclingTasks.add { modelPositionsGpuBuffer.cleanup() }

                    if(animated) {
                        animationDataBuffer.position(instance * animationDataPaddedSize)
                        animationDataBuffer.flip()
                        animationDataGpuBuffer!!.upload(animationDataBuffer)

                        frame.recyclingTasks.add { animationDataGpuBuffer.cleanup() }
                    }
                }

                handle(nonAnimatedInstances.entries, false)
                handle(animatedInstances.entries, true)
            }

            frame.recyclingTasks.add {
                bindingContexts.forEach { it.recycle() }
            }

            MemoryUtil.memFree(modelPositionsBuffer)
            MemoryUtil.memFree(animationDataBuffer)
            MemoryStack.stackPop()
        }

        override fun cleanup() {
            specializedPipelines.values.forEach(Cleanable::cleanup)
        }
    }

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer<VkModelsIR>.() -> Unit): VulkanDispatchingSystem.Drawer<VkModelsIR> = Drawer(pass, drawerInitCode)

    override fun cleanup() {
        //gpuUploadedModels.values.forEach(Cleanable::cleanup)
        //gpuUploadedModels.clear()
        gpuUploadedMeshes.values.forEach(Cleanable::cleanup)
        gpuUploadedMeshes.clear()

        sampler.cleanup()
    }

    override fun sort(representations: Sequence<ModelInstance>, drawers: List<VulkanDispatchingSystem.Drawer<VkModelsIR>>, workForDrawers: MutableMap<VulkanDispatchingSystem.Drawer<VkModelsIR>, VkModelsIR>) {
        val internalRepresentations: MutableMap<Mesh, MeshInstances> = mutableMapOf()
        //val perMaterial = mutableMapOf<MeshMaterial, MaterialInstance>()

        // First explode the model instances into constituent meshes and then bucket them up per material
        for (modelInstance in representations) {
            for ((i, mesh) in modelInstance.model.meshes.withIndex()) {
                if (modelInstance.meshesMask and (1 shl i) == 0)
                    continue

                val meshInstances = internalRepresentations.getOrPut(mesh) { MeshInstances(mesh, mutableMapOf(), mutableMapOf()) }

                val animated = modelInstance.animator != null
                val material = modelInstance.materials[i] ?: mesh.material

                val materialInstances = (
                        if (animated) meshInstances.animated
                        else meshInstances.instances).getOrPut(material) { MeshMaterialInstances(material, mutableListOf()) }

                materialInstances.instances.add(modelInstance)
            }
        }

        // Then submit these meshMaterialInstances elements into the appropriate drawers
        for ((mesh, meshInstances) in internalRepresentations) {
            for (drawer in drawers) {
                val filteredMeshInstances = MeshInstances(mesh, mutableMapOf(), mutableMapOf())
                for((material, materialGroupedInstances) in meshInstances.instances) {
                    if(material.tag == (drawer as VulkanModelsDispatcher.Drawer).materialTag)
                        filteredMeshInstances.instances.put(material, materialGroupedInstances)
                }

                for((material, materialGroupedInstances) in meshInstances.animated) {
                    if(material.tag == (drawer as VulkanModelsDispatcher.Drawer).materialTag)
                        filteredMeshInstances.animated.put(material, materialGroupedInstances)
                }

                workForDrawers.getOrPut(drawer) { mutableListOf() }.add(filteredMeshInstances)
            }
        }
        /*if ((drawer as VulkanModelsDispatcher.Drawer).materialTag == meshMaterialInstance.material.tag) {
            workForDrawers.getOrPut(drawer) { mutableListOf() }.add(meshMaterialInstance)
        }*/
    }
}

/** Data structure for the generic models renderer: a list of mesh instances ... **/
private typealias VkModelsIR = MutableList<MeshInstances>

/** Instances of meshes are grouped in animated/not animated lists */
data class MeshInstances(val mesh: Mesh, val instances: MutableMap<MeshMaterial, MeshMaterialInstances>, val animated: MutableMap<MeshMaterial, MeshMaterialInstances>)

/** Instances of meshes are further grouped by material */
data class MeshMaterialInstances(val material: MeshMaterial, val instances: MutableList<ModelInstance>)

//data class MeshInstance(val mesh: Mesh, val material: MeshMaterial, val modelInstance: ModelInstance)
//data class MaterialInstance(val material: MeshMaterial, val instances: MutableMap<Mesh, MutableList<ModelInstance>>, val animatedInstances: MutableMap<Mesh, MutableList<ModelInstance>>)

//data class MeshInstances(val mesh: Mesh, val instances: MutableList<ModelInstance>)
//data class MeshInstance(val mesh: Mesh, val instance: ModelInstance)