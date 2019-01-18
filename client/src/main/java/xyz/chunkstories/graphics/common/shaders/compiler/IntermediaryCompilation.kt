package xyz.chunkstories.graphics.common.shaders.compiler

import graphics.scenery.spirvcrossj.*

import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.GLSLResource
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampler2D
import xyz.chunkstories.graphics.common.shaders.SpirvCrossHelper.spirvStageInt

fun ShaderCompiler.buildIntermediaryStructure(stages: Map<ShaderStage, String>): IntermediaryCompilationResults {
    libspirvcrossj.initializeProcess()
    val ressources = libspirvcrossj.getDefaultTBuiltInResource()

    val tProgram = TProgram()
    val tShaders = stages.mapValues {(stage, shaderCode) ->
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

    val compilers = stages.mapValues {(stage, _) ->
        val intermediate = tProgram.getIntermediate(stage.spirvStageInt)
        val intVec = IntVec()
        libspirvcrossj.glslangToSpv(intermediate, intVec)

        //logger.debug("intermediary: ${intVec.size()} spirv bytes generated")

        CompilerGLSL(intVec)
    }

    return IntermediaryCompilationResults(tProgram, tShaders, compilers)
}

data class IntermediaryCompilationResults(val tProgram: TProgram, val tShaders: Map<ShaderStage, TShader>, val compilers: Map<ShaderStage, CompilerGLSL>)

fun ShaderCompiler.createShaderResources(intermediarCompilationResults: IntermediaryCompilationResults) : List<GLSLResource> {
    val resources = mutableListOf<GLSLResource>()

    for((stage, compiler) in intermediarCompilationResults.compilers) {
        val stageResources = compiler.shaderResources

        for(i in 0 until stageResources.sampledImages.size().toInt()) {
            val sampledImage = stageResources.sampledImages[i]
            val type = compiler.getType(sampledImage.typeId)
            val imageType = type.image
            //println("i:$i $sampledImage ${sampledImage.name} ${sampledImage.typeId} ${sampledImage.baseTypeId}")
            //println("$type ${type.array.size()} ${type.basetype} ${type.typeAlias} ${type.parentType} ${type.vecsize} ${type.columns} ${type.image} ${type.memberTypes}")
            //println("${imageType.arrayed} ${imageType.dim} ${imageType.depth} ${imageType.access} ${imageType.type} ${imageType.format}")

            val sampledImageName = sampledImage.name
            val arraySize = Array(type.array.size().toInt()) { type.array[it].toInt() }.toList().getOrNull(0) ?: 0
            /** https://www.khronos.org/registry/spir-v/specs/1.0/SPIRV.html#Dim */
            val dimensionality = imageType.dim
            //TODO handle those:
            val shadowSampler = imageType.depth
            val arrayTexture = imageType.arrayed

            // Don't duplicate resources
            if(resources.find { it is GLSLUniformSampler2D && it.name == sampledImageName } != null)
                continue

            val setSlot: Int; val binding: Int

            when(dialect) {
                GLSLDialect.VULKAN -> {
                    setSlot = if(sampledImageName == "virtualTextures") 0 else 1
                    binding = (resources.filter { it.descriptorSetSlot == setSlot }.maxBy { it.binding }?.binding ?: -1) + 1
                }
                GLSLDialect.OPENGL4 -> {
                    setSlot = 0
                    binding = resources.size
                }
            }

            //TODO handle other dimensionalities
            resources.add(when(dimensionality) {
                1 -> GLSLUniformSampler2D(sampledImageName, setSlot, binding, arraySize)
                else -> throw Exception("Not handled yet")
            })
        }

        for(i in 0 until stageResources.uniformBuffers.size().toInt()) {
            val uniformBuffer = stageResources.uniformBuffers[i]
            //TODO
        }

        //TODO SSBO
    }

    return resources
}

fun ShaderCompiler.addDecorations(intermediarCompilationResults: IntermediaryCompilationResults, glslResources: List<GLSLResource>) {
    for((stage, compiler) in intermediarCompilationResults.compilers) {
        val stageResources = compiler.shaderResources

        for (i in 0 until stageResources.sampledImages.size().toInt()) {
            val spirvResource = stageResources.sampledImages[i]
            val glslResource = glslResources.find { it.name == spirvResource.name } as GLSLUniformSampler2D

            compiler.setDecoration(spirvResource.id, Decoration.DecorationDescriptorSet, glslResource.descriptorSetSlot.toLong())
            compiler.setDecoration(spirvResource.id, Decoration.DecorationBinding, glslResource.binding.toLong())
        }

        //TODO UBOS/SSBOS
    }
}

fun ShaderCompiler.toIntermediateGLSL(intermediarCompilationResults: IntermediaryCompilationResults): Map<ShaderStage, String> {
    return intermediarCompilationResults.compilers.mapValues {(stage, compiler) ->
        val options = CompilerGLSL.Options()

        when (dialect) {
            GLSLDialect.OPENGL4 -> {
                options.version = 400L
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