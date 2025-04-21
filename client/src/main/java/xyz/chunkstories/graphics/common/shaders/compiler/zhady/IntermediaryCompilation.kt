package xyz.chunkstories.graphics.common.shaders.compiler.zhady

import de.unisaarland.zhady.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.*
import sun.misc.Unsafe
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import java.lang.reflect.Field
import kotlin.io.path.pathString
import kotlin.io.path.writeText

data class IntermediaryCompilationResults(val tShaders: Map<ShaderStage, SWIGTYPE_p_Module>)

fun ShaderCompiler.buildIntermediaryStructure(stages: Map<ShaderStage, String>, spirv_13: Boolean): IntermediaryCompilationResults {
    val tShaders = stages.mapValues { (stage, shaderCode) ->
        //val module: SWIGTYPE_p_Module = SWIGTYPE_p_Module(SWIGTYPE_p_Module_.getCPtr(shady.new_module(arena, "chunkstories_${stage.toString()}_shader")), false)
        val vccConfig = vcc.vcc_init_config(pCompilerConfig)
        vccConfig.include_path = "C:\\msys64\\home\\Gob\\git\\shady\\build\\share\\vcc\\include/"
        val tmpFile = kotlin.io.path.createTempFile(suffix = ".cpp");
        tmpFile.writeText(shaderCode)
        println("vcc_shader cpp tmpfile: ${tmpFile}")

        val filenames = listOf(tmpFile.pathString, "--std=c++20", "-O3", "-fno-slp-vectorize", "-fno-vectorize")
        val longs = stackMallocLong(filenames.size)
        for (filename in filenames) {
            longs.put(memAddress(stackUTF8(filename)))
        }
        longs.flip()
        val filenames_pstring = SWIGTYPE_p_String(memAddress(longs), false)
        vcc.vcc_run_clang(vccConfig, SWIGTYPE_p_String(memAddress(stackUTF8(tmpFile.pathString)), false))
        val target_config: TargetConfig = shady.shd_default_target_config();
        shady.shd_driver_configure_target(target_config, driverConfig)
        var module = vcc.vcc_parse_back_into_module(pCompilerConfig, SWIGTYPE_p_TargetConfig(TargetConfig.getCPtr(target_config), false), vccConfig, SWIGTYPE_p_String(memAddress(stackLongs(memAddress(stackUTF8("mah module brah")))), false))
        val pModule = SWIGTYPE_p_p_Module_(memAddress(stackLongs(SWIGTYPE_p_Module.getCPtr(module))), false)
        shady.shd_driver_compile(driverConfig, target_config, SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false))
        //shady.dump_module(SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false))
        val old_arena = shady.shd_module_get_arena(SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false))
        shady.shd_destroy_ir_arena(old_arena)

        module = SWIGTYPE_p_Module(unsafe.getLong(SWIGTYPE_p_p_Module_.getCPtr(pModule)), false)
        //shady.dump_module(SWIGTYPE_p_Module_(SWIGTYPE_p_Module.getCPtr(module), false))
        module

        //vcc.destroy_vcc_options(vccConfig)

        /*val tShader = TShader(stage.spirvStageInt)

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
        tShader*/
    }

    /*val link = tProgram.link(EShMessages.EShMsgDefault)
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

        stackPush().use {
            val ptr = stackPointers(1)
            Spvc.spvc_context_create_compiler(ctx, Spvc.SPVC_BACKEND_GLSL, intermediate, 0, ptr)
            ptr[0]
        }
    }
    */
    return IntermediaryCompilationResults(tShaders)
}

fun ShaderCompiler.toIntermediateGLSL(intermediarCompilationResults: IntermediaryCompilationResults): Map<ShaderStage, String> {
    /*return intermediarCompilationResults.compilers.mapValues { (stage, compiler) ->
        val optionsBuf = memAllocPointer(1)
        Spvc.spvc_compiler_create_compiler_options(compiler, optionsBuf)
        when (dialect) {
            GLSLDialect.OPENGL -> {
                Spvc.spvc_compiler_options_set_uint(optionsBuf[0], Spvc.SPVC_COMPILER_OPTION_GLSL_VERSION, 330)
                Spvc.spvc_compiler_options_set_bool(optionsBuf[0], Spvc.SPVC_COMPILER_OPTION_GLSL_VULKAN_SEMANTICS, false)
                Spvc.spvc_compiler_options_set_bool(optionsBuf[0], Spvc.SPVC_COMPILER_OPTION_GLSL_ENABLE_420PACK_EXTENSION, false)
            }
            GLSLDialect.VULKAN -> {
                Spvc.spvc_compiler_options_set_uint(optionsBuf[0], Spvc.SPVC_COMPILER_OPTION_GLSL_VERSION, 450)
                Spvc.spvc_compiler_options_set_bool(optionsBuf[0], Spvc.SPVC_COMPILER_OPTION_GLSL_VULKAN_SEMANTICS, true)
            }
        }
        Spvc.spvc_compiler_install_compiler_options(compiler, optionsBuf[0])

        val sourceBuf = memAllocPointer(1)
        Spvc.spvc_compiler_compile(compiler, sourceBuf)
        val result = memUTF8(sourceBuf[0])
        memFree(optionsBuf)
        memFree(sourceBuf)
        result
    }*/
    TODO()
}