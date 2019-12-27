package xyz.chunkstories.graphics.common.shaders.compiler.spirvcross

import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.vulkan.textures.MagicTexturing

fun ShaderCompiler.createShaderResources(intermediarCompilationResults: IntermediaryCompilationResults, materialBoundResources: MutableSet<String>): Pair<List<GLSLInstancedInput>, List<GLSLResource>> {
    val resources = mutableListOf<GLSLResource>()
    val instancedInputs = mutableListOf<GLSLInstancedInput>()

    val assigner = newResourceLocationAssigner()

    for ((stage, compiler) in intermediarCompilationResults.compilers) {
        val stageResources = compiler.shaderResources

        var availableTextureUnit = 0

        for (i in 0 until stageResources.sampledImages.size().toInt()) {
            val sampledImage = stageResources.sampledImages[i]
            val type = compiler.getType(sampledImage.typeId)
            val imageType = type.image
            //println("i:$i $sampledImage ${sampledImage.name} ${sampledImage.typeId} ${sampledImage.baseTypeId}")
            //println("$type ${type.array.size()} ${type.basetype} ${type.typeAlias} ${type.parentType} ${type.vecsize} ${type.columns} ${type.image} ${type.memberTypes}")
            //println("${imageType.arrayed} ${imageType.dim} ${imageType.depth} ${imageType.access} ${imageType.type} ${imageType.format}")

            val sampledImageName = sampledImage.name
            val arraySize = Array(type.array.size().toInt()) { type.array[it].toInt() }.toList().getOrNull(0) ?: 1
            /** https://www.khronos.org/registry/spir-v/specs/1.0/SPIRV.html#Dim */
            val dimensionality = imageType.dim
            //TODO handle those:
            val shadowSampler = imageType.depth
            val arrayTexture = imageType.arrayed
            val combined = imageType.sampled

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLUniformSampledImage && it.name == sampledImageName } != null)
                continue

            val locator = assigner.assignSampledImage(sampledImageName, materialBoundResources)
            //val firstAvailableTextureUnit = resources.count { it is GLSLUniformSampledImage }
            val openglTextureUnits = (0 until arraySize).map { availableTextureUnit++ }.toIntArray()

            //TODO handle other dimensionalities
            if (arrayTexture) {
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

        for (i in 0 until stageResources.separateSamplers.size().toInt()) {
            val sampler = stageResources.separateSamplers[i]
            val samplerName = sampler.name

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
                continue

            val locator = assigner.assignSampler()
            resources.add(GLSLUniformSampler(samplerName, locator))
        }

        for (i in 0 until stageResources.separateImages.size().toInt()) {
            val separateImage = stageResources.separateImages[i]
            val type = compiler.getType(separateImage.typeId)
            val imageType = type.image
            //println("i:$i $sampledImage ${sampledImage.name} ${sampledImage.typeId} ${sampledImage.baseTypeId}")
            //println("$type ${type.array.size()} ${type.basetype} ${type.typeAlias} ${type.parentType} ${type.vecsize} ${type.columns} ${type.image} ${type.memberTypes}")
            //println("${imageType.arrayed} ${imageType.dim} ${imageType.depth} ${imageType.access} ${imageType.type} ${imageType.format}")

            val separateImageName = separateImage.name
            val arraySize =
                    if (separateImageName in MagicTexturing.magicTexturesNames)
                        0
                    else
                        Array(type.array.size().toInt()) { type.array[it].toInt() }.toList().getOrNull(0) ?: 1
            /** https://www.khronos.org/registry/spir-v/specs/1.0/SPIRV.html#Dim */
            val dimensionality = imageType.dim
            //TODO handle those:
            val shadowSampler = imageType.depth
            val arrayTexture = imageType.arrayed
            val combined = imageType.sampled

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLUniformImage2D && it.name == separateImageName } != null)
                continue

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

        uboLoop@
        for (i in 0 until stageResources.uniformBuffers.size().toInt()) {
            val uniformBuffer = stageResources.uniformBuffers[i]
            val uniformBufferName = uniformBuffer.name

            val split = uniformBufferName.split("_")

            //TODO support naked UBOs?
            if (split.size < 3)
                continue

            val ogStuffType = split[0]
            val type = split[1]
            val instanceName = split[2]

            //println("found ubo type: $type instanceName: $instanceName")

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLUniformBlock && it.name == uniformBufferName } != null)
                continue@uboLoop

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

        for (i in 0 until stageResources.storageBuffers.size().toInt()) {
            val storageBuffer = stageResources.storageBuffers[i]!!
            val storageBufferName = storageBuffer.name!!

            // If the ressource was already handled in another iteration of this per-shader-stage loop
            if (resources.find { it is GLSLShaderStorage && it.name == storageBufferName } != null)
                continue

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
            }
        }
    }

    return Pair(instancedInputs, resources)
}

interface ResourceLocationAssigner {
    fun assignSSBO(name: String, instanced: Boolean): ResourceLocator
    fun assignInlinedUBO(jvmStruct: GLSLType.JvmStruct): ResourceLocator
    fun assignSampler(): ResourceLocator
    fun assignSeperateImage(separateImageName: String, materialBoundResources: MutableSet<String>): ResourceLocator
    fun assignSampledImage(sampledImageName: String, materialBoundResources: MutableSet<String>): ResourceLocator
}