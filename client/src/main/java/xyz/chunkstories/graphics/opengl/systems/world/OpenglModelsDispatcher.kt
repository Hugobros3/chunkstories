package xyz.chunkstories.graphics.opengl.systems.world

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import xyz.chunkstories.api.graphics.Mesh
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.shaders.compiler.AvailableVertexInput
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.structs.SkeletalAnimationData
import xyz.chunkstories.graphics.opengl.*
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.graph.OpenglPassInstance
import xyz.chunkstories.graphics.opengl.shaders.OpenglShaderProgram
import xyz.chunkstories.graphics.opengl.shaders.bindShaderResources
import xyz.chunkstories.graphics.opengl.shaders.bindTexture
import xyz.chunkstories.graphics.opengl.shaders.bindStructuredUBO
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem
import xyz.chunkstories.world.WorldClientCommon

class OpenglModelsDispatcher(backend: OpenglGraphicsBackend) : OpenglDispatchingSystem<ModelInstance>(backend) {

    override val representationName: String = ModelInstance::class.java.canonicalName

    val gpuUploadedModels = mutableMapOf<Model, GpuModelData>()

    inner class GpuModelData(val model: Model) : Cleanable {
        val meshesData = model.meshes.map { GpuMeshesData(it) }

        inner class GpuMeshesData(val mesh: Mesh) {
            val attributesVertexBuffers = mesh.attributes.map {
                val data = it.data
                val vb = OpenglVertexBuffer(backend)
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

    inner class Drawer(pass: OpenglPass, initCode: Drawer.() -> Unit) : OpenglDispatchingSystem.Drawer<MeshInstance>(pass), ModelsRenderer {
        override lateinit var materialTag: String
        override lateinit var shader: String
        override var supportsAnimations: Boolean = false

        override val system: OpenglDispatchingSystem<*>
            get() = this@OpenglModelsDispatcher

        init {
            apply(initCode)
        }

        inner class SpecializedPipeline(val key: SpecializedPipelineKey) : Cleanable {
            val program: OpenglShaderProgram
            val pipeline: FakePSO

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

                val vertexInputs = vertexInputConfiguration {
                    for ((i, input) in compatibleInputs.withIndex()) {
                        val j = i

                        //TODO this doesn't account for real-world alignment requirements :(
                        val size = input.format.bytesPerComponent * input.components

                        attribute {
                            binding = j
                            locationName = input.name
                            format = Pair(input.format, input.components)
                            offset = 0
                        }

                        binding {
                            binding = j
                            stride = size
                            inputRate = InputRate.PER_VERTEX
                        }
                    }
                }

                pipeline = FakePSO(backend, program, pass, vertexInputs, FaceCullingMode.CULL_BACK)
            }

            override fun cleanup() {
                program.cleanup()
                pipeline.cleanup()
            }
        }

        val specializedPipelines = mutableMapOf<SpecializedPipelineKey, SpecializedPipeline>()

        override fun executeDrawingCommands(context: OpenglPassInstance, work: Sequence<MeshInstance>) {
            stackPush()

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

            for ((specializedPipelineKey, meshInstances) in buckets) {
                val specializedPipeline = specializedPipelines.getOrPut(specializedPipelineKey) { SpecializedPipeline(specializedPipelineKey) }

                val pipeline = specializedPipeline.pipeline

                val camera = context.taskInstance.camera
                val world = client.world as WorldClientCommon

                pipeline.bind()
                context.bindShaderResources(pipeline)

                var instance = 0
                for ((mesh, material, modelInstance) in meshInstances) {
                    val model: Model = modelInstance.model
                    val modelOnGpu = getGpuModelData(model)

                    val meshIndex = model.meshes.indexOf(mesh)
                    val meshOnGpu = modelOnGpu.meshesData[meshIndex]

                    for (inputIndex in specializedPipeline.compatibleInputsIndexes) {
                        val vertexBuffer = meshOnGpu.attributesVertexBuffers[inputIndex]
                        pipeline.bindVertexBuffer(inputIndex, vertexBuffer)
                    }

                    for (materialImageSlot in pipeline.program.glslProgram.materialImages) {
                        val textureName = material.textures[materialImageSlot.name] ?: "textures/notex.png"
                        pipeline.bindTexture(materialImageSlot.name, 0, backend.textures.getOrLoadTexture2D(textureName), null)
                    }

                    if (specializedPipeline.key.enableAnimations) {
                        val bonez = SkeletalAnimationData()
                        val animator = modelInstance.animator!!
                        for ((boneName, i) in mesh.boneIds!!) {
                            bonez.bones[i].set(animator.getBoneHierarchyTransformationMatrixWithOffset(boneName, animationTime))
                        }

                        pipeline.bindStructuredUBO("animationData", bonez)
                    }

                    pipeline.bindStructuredUBO("modelPosition", modelInstance.position)
                    glDrawArrays(GL_TRIANGLES, 0, mesh.vertices)

                    instance++

                    context.frame.stats.totalVerticesDrawn += mesh.vertices
                    context.frame.stats.totalDrawcalls++
                }
            }

            stackPop()
        }

        override fun cleanup() {
            specializedPipelines.values.forEach(Cleanable::cleanup)
        }
    }

    override fun createDrawerForPass(pass: OpenglPass, drawerInitCode: OpenglDispatchingSystem.Drawer<*>.() -> Unit)
     = Drawer(pass, drawerInitCode)

    data class MeshInstance(val mesh: Mesh, val material: MeshMaterial, val modelInstance: ModelInstance)

    override fun sort(instance: ModelInstance, drawers: Array<OpenglDispatchingSystem.Drawer<*>>, outputs: List<MutableList<Any>>) {
        for ((i, mesh) in instance.model.meshes.withIndex()) {
            if (instance.meshesMask and (1 shl i) == 0)
                continue

            val meshInstance = MeshInstance(mesh, instance.materials[i] ?: mesh.material, instance)

            for ((index, drawer) in drawers.withIndex()) {
                if ((drawer as Drawer).materialTag == meshInstance.material.tag) {
                    outputs[index].add(meshInstance)
                }
            }
        }
    }

    override fun cleanup() {
        gpuUploadedModels.values.forEach(Cleanable::cleanup)
        gpuUploadedModels.clear()
    }
}