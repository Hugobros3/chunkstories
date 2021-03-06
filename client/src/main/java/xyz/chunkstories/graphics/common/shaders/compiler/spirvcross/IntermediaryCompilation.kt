package xyz.chunkstories.graphics.common.shaders.compiler.spirvcross

import graphics.scenery.spirvcrossj.*
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.SpirvCrossHelper.spirvStageInt

fun ShaderCompiler.buildIntermediaryStructure(stages: Map<ShaderStage, String>, spirv_13: Boolean): IntermediaryCompilationResults {
    libspirvcrossj.initializeProcess()
    val ressources = libspirvcrossj.getDefaultTBuiltInResource()

    val tProgram = TProgram()
    val tShaders = stages.mapValues { (stage, shaderCode) ->
        val tShader = TShader(stage.spirvStageInt)

        tShader.setStrings(arrayOf(shaderCode), 1)
        tShader.setAutoMapBindings(true)
        tShader.setAutoMapLocations(true)

        if(spirv_13) {
            tShader.setEnvClient(EShTargetClientVersion.EShTargetVulkan_1_1, 100)
            tShader.setEnvTarget(EShTargetLanguage.EShTargetSpv, EShTargetLanguageVersion.EShTargetSpv_1_3)
        }

        var messages = EShMessages.EShMsgDefault
        messages = messages or EShMessages.EShMsgVulkanRules
        messages = messages or EShMessages.EShMsgSpvRules

        val parse = tShader.parse(ressources, 450, false, messages)
        if (!parse) {
            ShaderCompiler.logger.warn(tShader.infoLog)
            ShaderCompiler.logger.warn(tShader.infoDebugLog)

            val errorLine = tShader.infoLog?.split(":")?.getOrNull(2)?.toInt() ?: -1
            val area = 3
            val lines = shaderCode.lines()
            if (errorLine != -1) {
                for (lineNumber in Math.max(0, errorLine - area)..Math.min(errorLine + area, lines.size - 1))
                    println(lineNumber.toString().padStart(4, '0') + ": " + lines[lineNumber])
            }

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