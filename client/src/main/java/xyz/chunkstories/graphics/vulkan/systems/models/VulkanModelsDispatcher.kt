package xyz.chunkstories.graphics.vulkan.systems.models

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackLongs
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo
import xyz.chunkstories.api.graphics.Mesh
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.representations.RepresentationsGathered
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
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderTaskInstance
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.VulkanShaderResourcesContext
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.util.getVulkanFormat
import java.util.concurrent.ConcurrentHashMap

class VulkanModelsDispatcher(backend: VulkanGraphicsBackend) : VulkanDispatchingSystem<ModelInstance>(backend) {
    override val representationName: String = ModelInstance::class.java.canonicalName
    val sampler = VulkanSampler(backend)

    val gpuUploadedMeshes = ConcurrentHashMap<Mesh, GpuMeshData>()

    inner class GpuMeshData(val mesh: Mesh) : Cleanable {
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

    inner class Drawer(pass: VulkanPass, initCode: Drawer.() -> Unit) : VulkanDispatchingSystem.Drawer(pass), ModelsRenderer {
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

        private val ssboBufferSize = 1024 * 1024L

        override fun registerDrawingCommands(drawerWork: DrawerWork) {
            val work = drawerWork as ModelDrawerWork
            val context = work.drawerInstance.first
            val commandBuffer = work.cmdBuffer
            stackPush()

            val modelPositionsBuffer = MemoryUtil.memAlloc(ssboBufferSize.toInt())
            val animationDataBuffer = MemoryUtil.memAlloc(ssboBufferSize.toInt())

            val client = backend.window.client.ingame ?: return

            val realWorldTimeTruncated = (System.nanoTime() % 1000_000_000_000)
            val realWorldTimeMs = realWorldTimeTruncated / 1000_000
            val animationTime = (realWorldTimeMs / 1000.0) * 1000.0

            val bindingContexts = mutableListOf<VulkanShaderResourcesContext>()

            //for ((mesh, nonAnimatedInstances, animatedInstances) in work.queuedWork) {
            fun handle(mesh: Mesh, entries: Collection<Map.Entry<MeshMaterial, MeshMaterialInstances>>, animated: Boolean) {
                if (entries.isEmpty())
                    return

                // Obtain a specialized pipeline that works for this mesh and animation state
                val specializedPipelineKey = getSpecializedPipelineKeyForMeshAndShader(mesh, shader, supportsAnimations, animated)
                val specializedPipeline = specializedPipelines.getOrPut(specializedPipelineKey) { SpecializedPipeline(specializedPipelineKey) }
                val pipeline = specializedPipeline.pipeline
                val bindingContext = context.getBindingContext(pipeline)
                bindingContexts += bindingContext

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
                bindingContext.commitAndBind(commandBuffer)

                var instance = 0

                // TODO pool those allocations
                val modelPositionsGpuBuffer = VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
                //val modelPositionsBuffer = MemoryUtil.memAlloc(modelPositionsGpuBuffer.bufferSize.toInt())
                modelPositionsBuffer.clear()

                val modelPositionsInstancedInput = specializedPipeline.program.glslProgram.instancedInputs.find { it.name == "modelPosition" }!!
                val modelPositionsPaddedSize = getStd140AlignedSizeForStruct(modelPositionsInstancedInput.struct)


                val animationDataGpuBuffer = if (animated) VulkanBuffer(backend, ssboBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC) else null
                //val animationDataBuffer = if(animated) MemoryUtil.memAlloc(animationDataGpuBuffer!!.bufferSize.toInt()) else null
                animationDataBuffer.clear()

                val animationDataInstancedInput = if (animated) specializedPipeline.program.glslProgram.instancedInputs.find { it.name == "animationData" } else null
                val animationDataPaddedSize = if (animationDataInstancedInput != null) getStd140AlignedSizeForStruct(animationDataInstancedInput.struct) else 0


                // Get and bind mesh vertex data
                val meshOnGpu = getGpuMeshData(mesh)
                for (inputIndex in specializedPipeline.compatibleInputsIndexes) {
                    val vertexBuffer = meshOnGpu.attributesVertexBuffers[inputIndex]
                    vkCmdBindVertexBuffers(commandBuffer, inputIndex, stackLongs(vertexBuffer.handle), stackLongs(0))
                }

                for ((material, meshMaterialInstances) in entries) {
                    val perMaterialBindingContext = context.getBindingContext(pipeline)
                    bindingContexts += perMaterialBindingContext

                    val firstInstance = instance
                    var materialInstancesCount = 0

                    for (materialImageSlot in pipeline.program.glslProgram.materialImages) {
                        val textureName = material.textures[materialImageSlot.name] ?: "textures/notex.png"
                        perMaterialBindingContext.bindTextureAndSampler(materialImageSlot.name, backend.textures.getOrLoadTexture2D(textureName), sampler)
                        //println(pipeline.program.glslProgram)
                    }

                    for (modelInstance in meshMaterialInstances.instances) {
                        extractInterfaceBlock(modelPositionsBuffer, instance * modelPositionsPaddedSize, modelInstance.position, modelPositionsInstancedInput.struct)

                        if (animated) {
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
                    if (animated) {
                        perMaterialBindingContext.bindInstancedInput("animationData", animationDataGpuBuffer!!)
                    }
                    perMaterialBindingContext.commitAndBind(commandBuffer)


                    vkCmdDraw(commandBuffer, mesh.vertices, materialInstancesCount, 0, firstInstance)

                    context.frame.stats.totalVerticesDrawn += mesh.vertices
                    context.frame.stats.totalDrawcalls++
                }

                modelPositionsBuffer.position(instance * modelPositionsPaddedSize)
                modelPositionsBuffer.flip()
                modelPositionsGpuBuffer.upload(modelPositionsBuffer)

                context.frame.recyclingTasks.add { modelPositionsGpuBuffer.cleanup() }

                if (animated) {
                    animationDataBuffer.position(instance * animationDataPaddedSize)
                    animationDataBuffer.flip()
                    animationDataGpuBuffer!!.upload(animationDataBuffer)

                    context.frame.recyclingTasks.add { animationDataGpuBuffer.cleanup() }
                }
            }

            for ((mesh, materialInstances) in work.queuedWork) {
                handle(mesh, materialInstances.entries, false)
            }

            for ((mesh, materialInstances) in work.queuedWorkAnimated) {
                handle(mesh, materialInstances.entries, true)
            }

            context.frame.recyclingTasks.add {
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

    override fun createDrawerForPass(pass: VulkanPass, drawerInitCode: VulkanDispatchingSystem.Drawer.() -> Unit): VulkanDispatchingSystem.Drawer = Drawer(pass, drawerInitCode)

    override fun cleanup() {
        //gpuUploadedModels.values.forEach(Cleanable::cleanup)
        //gpuUploadedModels.clear()
        gpuUploadedMeshes.values.forEach(Cleanable::cleanup)
        gpuUploadedMeshes.clear()

        sampler.cleanup()
    }

    class ModelDrawerWork(drawerInstance: Pair<VulkanPassInstance, Drawer>): DrawerWork(drawerInstance) {
        val animationSupported = drawerInstance.second.supportsAnimations

        val queuedWork = mutableMapOf<Mesh, MutableMap<MeshMaterial, MeshMaterialInstances>>()
        val queuedWorkAnimated = mutableMapOf<Mesh, MutableMap<MeshMaterial, MeshMaterialInstances>>()

        override fun isEmpty() =queuedWork.isEmpty() && queuedWorkAnimated.isEmpty()
    }

    override fun sortWork(frame: VulkanFrame, drawers: Map<VulkanRenderTaskInstance, List<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>>>, maskedBuckets: Map<Int, RepresentationsGathered.Bucket>): Map<Pair<VulkanPassInstance, VulkanDispatchingSystem.Drawer>, DrawerWork> {
        val allDrawersPlusInstances = drawers.values.flatten().filterIsInstance<Pair<VulkanPassInstance, Drawer>>()

        val workForDrawers = allDrawersPlusInstances.associateWith {
            ModelDrawerWork(it)
        }

        // Very complicated confusing logic here
        // What we do is we go through each bucket of representations, and sort the representations to add them to the work queue of all the (passInstance, drawers) couples that can indeed draw them
        // for a drawer+instance combo to be eligible to draw a mesh, it needs a few conditions:
        // 1) the overall representation was submitted with a mask that matches the mask of the rendertask instance parent of the passinstance
        // 2) the mesh mask in the model didn't mask off that mesh
        // 3) the mesh material tag matches with the drawer materialTag
        for ((mask, bucket) in maskedBuckets) {
            @Suppress("UNCHECKED_CAST") val somewhatRelevantDrawers = drawers.filter { it.key.mask and mask != 0 }.flatMap { it.value } as List<Pair<VulkanPassInstance, Drawer>>
            @Suppress("UNCHECKED_CAST") val representations = bucket.representations as ArrayList<ModelInstance>

            val drawerRelevancyMap = mutableMapOf<DrawerRelevancyKey, List<ModelDrawerWork>>()

            for (modelInstance in representations) {
                for ((i, mesh) in modelInstance.model.meshes.withIndex()) {
                    if (modelInstance.meshesMask and (1 shl i) == 0)
                        continue

                    val animated = modelInstance.animator != null
                    val material = modelInstance.materials[i] ?: mesh.material

                    val relevantWorkQueues = drawerRelevancyMap.getOrPut(DrawerRelevancyKey(material.tag, animated)) {
                        somewhatRelevantDrawers.filter { it.second.materialTag == material.tag }.map { workForDrawers[it]!! }
                    }

                    for (queue in relevantWorkQueues) {
                        if (!animated || !queue.animationSupported)
                            queue.queuedWork.getOrPut(mesh) { mutableMapOf() }.getOrPut(material) { MeshMaterialInstances(material) }.instances.add(modelInstance)
                        else
                            queue.queuedWorkAnimated.getOrPut(mesh) { mutableMapOf() }.getOrPut(material) { MeshMaterialInstances(material) }.instances.add(modelInstance)
                    }
                }
            }
        }

        return workForDrawers.map { Pair(it.key, it.value) }.toMap()
    }
}

data class DrawerRelevancyKey(val meterialTag: String, val animated: Boolean)

/** Instances of meshes are further grouped by material */
data class MeshMaterialInstances(val material: MeshMaterial, val instances: MutableList<ModelInstance> = arrayListOf())