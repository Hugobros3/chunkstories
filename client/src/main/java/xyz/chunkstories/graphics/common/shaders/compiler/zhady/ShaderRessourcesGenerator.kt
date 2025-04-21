package xyz.chunkstories.graphics.common.shaders.compiler.spirvcross

import de.unisaarland.zhady.*
import org.lwjgl.system.MemoryUtil
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.common.shaders.compiler.zhady.IntermediaryCompilationResults
import xyz.chunkstories.graphics.vulkan.textures.GlobalTextures

interface ResourceLocationAssigner {
    fun assignSSBO(name: String, instanced: Boolean): ResourceLocator
    fun assignInlinedUBO(jvmStruct: GLSLType.JvmStruct): ResourceLocator
    fun assignSampler(): ResourceLocator
    fun assignSeperateImage(separateImageName: String, materialBoundResources: MutableSet<String>): ResourceLocator
    fun assignSampledImage(sampledImageName: String, materialBoundResources: MutableSet<String>): ResourceLocator
}

fun ShaderCompiler.createShaderResources(intermediarCompilationResults: IntermediaryCompilationResults, materialBoundResources: MutableSet<String>): Pair<List<GLSLInstancedInput>, List<GLSLResource>> {
    val resources = mutableListOf<GLSLResource>()
    val instancedInputs = mutableListOf<GLSLInstancedInput>()
    val assigner = newResourceLocationAssigner()

    for ((stage, module) in intermediarCompilationResults.tShaders) {
        // val stageResources = compiler.shaderResources

        var availableTextureUnit = 0

        fun handleUniformBuffer(uniformBuffer: Node_) {
            var uniformBufferName = shady.shd_get_exported_name(uniformBuffer)
            uniformBufferName = uniformBufferName.removePrefix("struct.")
            val split = uniformBufferName.split("_")

            //TODO support naked UBOs?
            if (split.size < 3)
                return

            val ogStuffType = split[0]
            val type = split[1]
            val instanceName = split[2]

            //println("found ubo type: $type instanceName: $instanceName")

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLUniformBlock && it.name == uniformBufferName } != null)
                return

            when (ogStuffType) {
                "uniformstructinlined" -> {
                    val jvmStruct = jvmGlslMappings.values.find { it.glslToken == type }!!

                    val locator = assigner.assignInlinedUBO(jvmStruct)
                    resources.add(GLSLUniformBlock(uniformBufferName, locator, instanceName, jvmStruct))
                }
                "instancedbuffer" -> {
                    val jvmStruct = jvmGlslMappings.values.find { it.glslToken == type }!!

                    if(dialect != GLSLDialect.OPENGL)
                        throw Exception("This hack is only done on the OpenGL backend")

                    val locator = assigner.assignInlinedUBO(jvmStruct)

                    val resource = GLSLUniformBlock(uniformBufferName, locator, instanceName, jvmStruct)
                    resources.add(resource)

                    val instancedInput = GLSLInstancedInput(instanceName, jvmStruct, resource)
                    instancedInputs.add(instancedInput)
                }
                else -> {
                    //TODO support naked UBOs?
                }
            }
        }

        fun handleSSBO(storageBuffer: Node_) {
            val storageBufferName = shady.shd_get_exported_name(storageBuffer)

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLShaderStorage && it.name == storageBufferName } != null)
                return

            // Earlier shader compiler stages will generate SSBOs to go alongside instanced inputs, and their name contains some information
            val split = storageBufferName.split("_")
            if (split.size >= 3 && split[0] == "instancedbuffer") {
                val type = split[1]
                val instanceName = split[2]

                val jvmStruct = jvmGlslMappings.values.find { it.glslToken == type }!!

                val locator = assigner.assignSSBO(name = storageBufferName, instanced = true)
                val ssboRessource = GLSLShaderStorage(storageBufferName, locator)
                val instancedInputRessource = GLSLInstancedInput(instanceName, jvmStruct, ssboRessource)
                instancedInputs.add(instancedInputRessource)
                resources.add(ssboRessource)
            } else {
                // Maybe this is just a normal SSBO !
                //TODO Implement this logic
                val locator = assigner.assignSSBO(name = storageBufferName, instanced = false)
                val ssboRessource = GLSLShaderStorage(storageBufferName, locator)
                resources.add(ssboRessource)
            }
        }

        fun handleImage(separateImage: Node_) {
            val type = separateImage.payload.global_variable.type
            assert(type.tag == NodeTag.ImageType_TAG)
            val imageType = type.payload.image_type
            //println("i:$i $sampledImage ${sampledImage.name} ${sampledImage.typeId} ${sampledImage.baseTypeId}")
            //println("$type ${type.array.size()} ${type.basetype} ${type.typeAlias} ${type.parentType} ${type.vecsize} ${type.columns} ${type.image} ${type.memberTypes}")
            //println("${imageType.arrayed} ${imageType.dim} ${imageType.depth} ${imageType.access} ${imageType.type} ${imageType.format}")

            val separateImageName = shady.shd_get_exported_name(separateImage)
            val arraySize =
                if (separateImageName in GlobalTextures.magicTexturesNames)
                    0
                else
                    1 // TODO(parse array type) Array(type.array.size().toInt()) { type.array[it].toInt() }.toList().getOrNull(0) ?: 1
            /** https://www.khronos.org/registry/spir-v/specs/1.0/SPIRV.html#Dim */
            val dimensionality = imageType.dim.toInt()
            //TODO handle those:
            val shadowSampler = imageType.depth
            val arrayTexture = imageType.arrayed
            val combined = imageType.sampled

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLUniformImage2D && it.name == separateImageName } != null)
                return

            /*val setSlot: Int
            val binding: Int

            when (dialect) {
                GLSLDialect.VULKAN -> {
                    setSlot = when (separateImageName) {
                        in MagicTexturing.magicTexturesNames -> 0
                        in materialBoundResources -> UniformUpdateFrequency.ONCE_PER_BATCH.ordinal + 2
                        else -> 1
                    }
                    binding =
                            if (separateImageName in MagicTexturing.magicTexturesNames) 1
                            else
                                (resources.filter { it.descriptorSetSlot == setSlot }.maxBy { it.binding }?.binding
                                        ?: -1) + 1
                }
                GLSLDialect.OPENGL -> {
                    setSlot = 0
                    binding = resources.size
                }
            }*/
            val locator = assigner.assignSeperateImage(separateImageName, materialBoundResources)

            //TODO handle other dimensionalities
            resources.add(when (dimensionality) {
                1 -> GLSLUniformImage2D(separateImageName, locator, arraySize)
                else -> throw Exception("Not handled yet")
            })
        }

        fun handleSampledImage(sampledImage: Node_) {
            // val sampledImage = stageResources.sampledImages[i]
            val type = sampledImage.payload.global_variable.type
            assert(type.tag == NodeTag.SampledImageType_TAG)
            val sampledImageType = type.payload.sampled_image_type
            val imageType = sampledImageType.image_type.payload.image_type
            //println("i:$i $sampledImage ${sampledImage.name} ${sampledImage.typeId} ${sampledImage.baseTypeId}")
            //println("$type ${type.array.size()} ${type.basetype} ${type.typeAlias} ${type.parentType} ${type.vecsize} ${type.columns} ${type.image} ${type.memberTypes}")
            //println("${imageType.arrayed} ${imageType.dim} ${imageType.depth} ${imageType.access} ${imageType.type} ${imageType.format}")

            val sampledImageName = shady.shd_get_exported_name(sampledImage)
            val arraySize = 1 //TODO("parse arrays here") Array(type.array.size().toInt()) { type.array[it].toInt() }.toList().getOrNull(0) ?: 1
            /** https://www.khronos.org/registry/spir-v/specs/1.0/SPIRV.html#Dim */
            val dimensionality = imageType.dim.toInt()
            //TODO handle those:
            val shadowSampler = imageType.depth
            val arrayTexture = imageType.arrayed
            val combined = imageType.sampled

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLUniformSampledImage && it.name == sampledImageName } != null)
                return

            val locator = assigner.assignSampledImage(sampledImageName, materialBoundResources)
            //val firstAvailableTextureUnit = resources.count { it is GLSLUniformSampledImage }
            val openglTextureUnits = (0 until arraySize).map { availableTextureUnit++ }.toIntArray()

            //TODO handle other dimensionalities
            if (arrayTexture != 0L) {
                resources.add(when (dimensionality) {
                    1 -> GLSLUniformSampledImage2DArray(sampledImageName, locator, openglTextureUnits)
                    else -> throw Exception("Not handled yet")
                })
            } else {
                resources.add(when (dimensionality) {
                    1 -> GLSLUniformSampledImage2D(sampledImageName, locator, openglTextureUnits, arraySize)
                    2 -> GLSLUniformSampledImage3D(sampledImageName, locator, openglTextureUnits, arraySize)
                    3 -> GLSLUniformSampledImageCubemap(sampledImageName, locator, openglTextureUnits)
                    else -> throw Exception("Not handled yet")
                })
            }
        }

        fun handleSampler(sampler: Node_) {
            val samplerName = shady.shd_get_exported_name(sampler)

            /*val setSlot: Int
            val binding: Int

            //TODO there is no reason all samplers should go here!
            when (dialect) {
                GLSLDialect.VULKAN -> {
                    setSlot = 0
                    binding = 0
                }
                GLSLDialect.OPENGL -> {
                    setSlot = 0
                    binding = resources.size
                }
            }*/

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLUniformSampler && it.name == samplerName } != null)
                return

            val locator = assigner.assignSampler()
            resources.add(GLSLUniformSampler(samplerName, locator))
        }

        val decls = shady.shd_module_get_all_exported(SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false))
        val declsNodesBuffer = MemoryUtil.memPointerBuffer(SWIGTYPE_p_p_Node_.getCPtr(decls.nodes), decls.count.toInt())

        for (i in 0 until decls.count.toInt()) {
            val decl = Node_(declsNodesBuffer.get(i), false)
            shady.shd_dump(decl)
            if (decl.tag == NodeTag.GlobalVariable_TAG) {
                when (decl.payload.global_variable.address_space) {
                    AddressSpace.AsUniform -> handleUniformBuffer(decl)
                    AddressSpace.AsUniformConstant -> {
                        val ptrType = decl.type
                        assert(ptrType.tag == NodeTag.PtrType_TAG)
                        val type = ptrType.payload.ptr_type.pointed_type
                        when (type.tag) {
                            NodeTag.ImageType_TAG -> handleImage(decl)
                            NodeTag.SampledImageType_TAG -> handleSampledImage(decl)
                            NodeTag.SamplerType_TAG -> handleSampler(decl)
                            else -> continue
                        }
                    }
                    AddressSpace.AsShaderStorageBufferObject -> handleSSBO(decl)
                    else -> continue
                }
            }
        }
    }

    return Pair(instancedInputs, resources)
}