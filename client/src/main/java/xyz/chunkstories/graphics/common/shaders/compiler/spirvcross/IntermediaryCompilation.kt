package xyz.chunkstories.graphics.common.shaders.compiler.spirvcross

import graphics.scenery.spirvcrossj.*
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.common.shaders.compiler.preprocessing.updateFrequency
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.SpirvCrossHelper.spirvStageInt
import xyz.chunkstories.graphics.vulkan.textures.MagicTexturing.Companion.magicTexturesNames

fun ShaderCompiler.buildIntermediaryStructure(stages: Map<ShaderStage, String>, dumpCodeOnError: Boolean): IntermediaryCompilationResults {
    libspirvcrossj.initializeProcess()
    val ressources = libspirvcrossj.getDefaultTBuiltInResource()

    val tProgram = TProgram()
    val tShaders = stages.mapValues { (stage, shaderCode) ->
        val tShader = TShader(stage.spirvStageInt)

        tShader.setStrings(arrayOf(shaderCode), 1)
        tShader.setAutoMapBindings(true)
        tShader.setAutoMapLocations(true)

        var messages = EShMessages.EShMsgDefault
        messages = messages or EShMessages.EShMsgVulkanRules
        messages = messages or EShMessages.EShMsgSpvRules

        val parse = tShader.parse(ressources, 450, false, messages)
        if (!parse) {
            ShaderCompiler.logger.warn(tShader.infoLog)
            ShaderCompiler.logger.warn(tShader.infoDebugLog)

            if (dumpCodeOnError)
                println(shaderCode)
            throw Exception("Failed to parse stage $stage of the shader program")
        }

        tProgram.addShader(tShader)
        tShader
    }

    val link = tProgram.link(EShMessages.EShMsgDefault)
    val ioMap = tProgram.mapIO()

    if (!link || !ioMap) {
        ShaderCompiler.logger.warn(tProgram.infoLog)
        ShaderCompiler.logger.warn(tProgram.infoDebugLog)

        throw Exception("Failed to link or map stages of the shader program")
    }

    tProgram.buildReflection()
    libspirvcrossj.finalizeProcess()

    val compilers = stages.mapValues { (stage, _) ->
        val intermediate = tProgram.getIntermediate(stage.spirvStageInt)
        val intVec = IntVec()
        libspirvcrossj.glslangToSpv(intermediate, intVec)

        //logger.debug("intermediary: ${intVec.size()} spirv bytes generated")

        CompilerGLSL(intVec)
    }

    return IntermediaryCompilationResults(tProgram, tShaders, compilers)
}

data class IntermediaryCompilationResults(val tProgram: TProgram, val tShaders: Map<ShaderStage, TShader>, val compilers: Map<ShaderStage, CompilerGLSL>)

fun ShaderCompiler.createShaderResources(intermediarCompilationResults: IntermediaryCompilationResults, materialBoundResources: MutableSet<String>): Pair<List<GLSLInstancedInput>, List<GLSLResource>> {
    val resources = mutableListOf<GLSLResource>()
    val instancedInputs = mutableListOf<GLSLInstancedInput>()

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

            // Don't duplicate resources
            if (resources.find { it is GLSLUniformSampledImage && it.name == sampledImageName } != null)
                continue

            val setSlot: Int
            val binding: Int

            when (dialect) {
                GLSLDialect.VULKAN -> {
                    setSlot = when (sampledImageName) {
                        in materialBoundResources -> UniformUpdateFrequency.ONCE_PER_BATCH.ordinal + 2
                        else -> 1
                    }
                    binding = (resources.filter { it.descriptorSetSlot == setSlot }.maxBy { it.binding }?.binding
                            ?: -1) + 1
                }
                GLSLDialect.OPENGL -> {
                    setSlot = 0
                    binding = resources.size
                }
            }

            //val firstAvailableTextureUnit = resources.count { it is GLSLUniformSampledImage }
            val openglTextureUnits = (0 until arraySize).map { availableTextureUnit++ }.toIntArray()

            //TODO handle other dimensionalities
            if (arrayTexture) {
                resources.add(when (dimensionality) {
                    1 -> GLSLUniformSampledImage2DArray(sampledImageName, setSlot, binding, openglTextureUnits)
                    else -> throw Exception("Not handled yet")
                })
            } else {
                resources.add(when (dimensionality) {
                    1 -> GLSLUniformSampledImage2D(sampledImageName, setSlot, binding, openglTextureUnits, arraySize)
                    2 -> GLSLUniformSampledImage3D(sampledImageName, setSlot, binding, openglTextureUnits, arraySize)
                    3 -> GLSLUniformSampledImageCubemap(sampledImageName, setSlot, binding, openglTextureUnits)
                    else -> throw Exception("Not handled yet")
                })
            }
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
                    if (separateImageName in magicTexturesNames)
                        0
                    else
                        Array(type.array.size().toInt()) { type.array[it].toInt() }.toList().getOrNull(0) ?: 1
            /** https://www.khronos.org/registry/spir-v/specs/1.0/SPIRV.html#Dim */
            val dimensionality = imageType.dim
            //TODO handle those:
            val shadowSampler = imageType.depth
            val arrayTexture = imageType.arrayed
            val combined = imageType.sampled

            // Don't duplicate resources
            if (resources.find { it is GLSLUniformImage2D && it.name == separateImageName } != null)
                continue

            val setSlot: Int
            val binding: Int

            when (dialect) {
                GLSLDialect.VULKAN -> {
                    setSlot = when (separateImageName) {
                        in magicTexturesNames -> 0
                        in materialBoundResources -> UniformUpdateFrequency.ONCE_PER_BATCH.ordinal + 2
                        else -> 1
                    }
                    binding =
                            if (separateImageName in magicTexturesNames) 1
                            else
                                (resources.filter { it.descriptorSetSlot == setSlot }.maxBy { it.binding }?.binding
                                        ?: -1) + 1
                }
                GLSLDialect.OPENGL -> {
                    setSlot = 0
                    binding = resources.size
                }
            }

            //TODO handle other dimensionalities
            resources.add(when (dimensionality) {
                1 -> GLSLUniformImage2D(separateImageName, setSlot, binding, arraySize)
                else -> throw Exception("Not handled yet")
            })
        }

        for (i in 0 until stageResources.separateSamplers.size().toInt()) {
            val sampler = stageResources.separateSamplers[i]
            val samplerName = sampler.name

            val setSlot: Int
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
            }

            if (resources.find { it is GLSLUniformSampler && it.name == samplerName } != null)
                continue

            resources.add(GLSLUniformSampler(samplerName, setSlot, binding))
        }

        uboLoop@
        for (i in 0 until stageResources.uniformBuffers.size().toInt()) {
            val uniformBuffer = stageResources.uniformBuffers[i]
            val uniformBufferName = uniformBuffer.name

            val split = uniformBufferName.split("_")

            if(split.size < 3)
                continue

            val ogStuffType = split[0]
            val type = split[1]
            val instanceName = split[2]

            //println("found ubo type: $type instanceName: $instanceName")

            when(ogStuffType) {
                "uniformstructinlined" -> {
                    if (resources.find { it is GLSLUniformBlock && it.name == instanceName } != null)
                        continue@uboLoop

                    val jvmStruct = jvmGlslMappings.values.find { it.glslToken == type }!!

                    val setSlot: Int
                    val binding: Int

                    when (dialect) {
                        GLSLDialect.VULKAN -> {
                            setSlot = jvmStruct.kClass.updateFrequency().ordinal + 2
                            binding = (resources.filter { it.descriptorSetSlot == setSlot }.maxBy { it.binding }?.binding
                                    ?: -1) + 1
                        }
                        GLSLDialect.OPENGL -> {
                            setSlot = 0
                            binding = resources.size
                        }
                    }

                    resources.add(GLSLUniformBlock(instanceName, uniformBufferName, setSlot, binding, jvmStruct))
                }
                "instancedbuffer" -> {
                    if (resources.find { it is GLSLUniformBlock && it.name == instanceName } != null)
                        continue@uboLoop

                    val jvmStruct = jvmGlslMappings.values.find { it.glslToken == type }!!

                    val setSlot: Int
                    val binding: Int

                    when (dialect) {
                        GLSLDialect.VULKAN -> {
                            setSlot = jvmStruct.kClass.updateFrequency().ordinal + 2
                            binding = (resources.filter { it.descriptorSetSlot == setSlot }.maxBy { it.binding }?.binding
                                    ?: -1) + 1
                        }
                        GLSLDialect.OPENGL -> {
                            setSlot = 0
                            binding = resources.size
                        }
                    }

                    val resource = GLSLUniformBlock(instanceName, uniformBufferName, setSlot, binding, jvmStruct)
                    resources.add(resource)

                    val instancedInput = GLSLInstancedInput(instanceName, jvmStruct, resource)
                    instancedInputs.add(instancedInput)
                }
            }

        }

        //TODO SSBO
        for (i in 0 until stageResources.storageBuffers.size().toInt()) {
            val storageBuffer = stageResources.storageBuffers[i]
            val storageBufferName = storageBuffer.name

            val split = storageBufferName.split("_")

            if (split.size < 3 || split[0] != "instancedbuffer") // Maybe this is just a normal SSBO !
                continue

            val type = split[1]
            val instanceName = split[2]

            if (resources.find { it is GLSLShaderStorage && it.name == instanceName } != null)
                continue

            val jvmStruct = jvmGlslMappings.values.find { it.glslToken == type }!!

            val setSlot: Int
            val binding: Int

            when (dialect) {
                GLSLDialect.VULKAN -> {
                    setSlot = UniformUpdateFrequency.ONCE_PER_BATCH.ordinal + 2
                    binding = (resources.filter { it.descriptorSetSlot == setSlot }.maxBy { it.binding }?.binding
                            ?: -1) + 1
                }
                GLSLDialect.OPENGL -> {
                    setSlot = 0
                    binding = resources.size
                }
            }

            val glslResource = GLSLShaderStorage(instanceName, setSlot, binding)
            val instancedInput = GLSLInstancedInput(instanceName, jvmStruct, glslResource)
            instancedInputs.add(instancedInput)
            resources.add(glslResource)
        }
    }

    return Pair(instancedInputs, resources)
}

fun ShaderCompiler.addDecorations(intermediarCompilationResults: IntermediaryCompilationResults, glslResources: List<GLSLResource>, glslInstancedInputs: List<GLSLInstancedInput>) {
    for ((stage, compiler) in intermediarCompilationResults.compilers) {
        val stageResources = compiler.shaderResources

        fun decorate(spirvResource: Resource, glslResource: GLSLResource) {
            when (dialect) {
                GLSLDialect.VULKAN -> {
                    compiler.setDecoration(spirvResource.id, Decoration.DecorationDescriptorSet, glslResource.descriptorSetSlot.toLong())
                    compiler.setDecoration(spirvResource.id, Decoration.DecorationBinding, glslResource.binding.toLong())
                }
                GLSLDialect.OPENGL -> {
                    compiler.setDecoration(spirvResource.id, Decoration.DecorationLocation, glslResource.binding.toLong())
                }
            }
        }

        for (i in 0 until stageResources.sampledImages.size().toInt()) {
            val spirvResource = stageResources.sampledImages[i]
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLResource

            decorate(spirvResource, glslResource)
        }

        for (i in 0 until stageResources.separateImages.size().toInt()) {
            val spirvResource = stageResources.separateImages[i]
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLUniformImage2D

            decorate(spirvResource, glslResource)
        }

        for (i in 0 until stageResources.separateSamplers.size().toInt()) {
            val spirvResource = stageResources.separateSamplers[i]
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLUniformSampler

            decorate(spirvResource, glslResource)
        }

        for (i in 0 until stageResources.uniformBuffers.size().toInt()) {
            val spirvResource = stageResources.uniformBuffers[i]
            val instanceName = spirvResource.name.split("_")[2]

            val glslResource = glslResources.find { it.name == instanceName } as GLSLUniformBlock

            decorate(spirvResource, glslResource)
        }

        //TODO SSBOS
        for (i in 0 until stageResources.storageBuffers.size().toInt()) {
            val spirvResource = stageResources.storageBuffers[i]
            val instanceName = spirvResource.name.split("_")[2]

            val glslInstancedInput = glslInstancedInputs.find { it.name == instanceName }!!
            val glslResource = glslInstancedInput.associatedResource as GLSLShaderStorage

            decorate(spirvResource, glslResource)
        }
    }
}

fun ShaderCompiler.toIntermediateGLSL(intermediarCompilationResults: IntermediaryCompilationResults): Map<ShaderStage, String> {
    return intermediarCompilationResults.compilers.mapValues { (stage, compiler) ->
        val options = CompilerGLSL.Options()

        when (dialect) {
            GLSLDialect.OPENGL -> {
                options.version = 330L
                options.vulkanSemantics = false
                options.enable420packExtension = false
            }
            GLSLDialect.VULKAN -> {
                options.version = 450L
                options.vulkanSemantics = true
            }
        }
        compiler.options = options

        compiler.compile()
    }
}