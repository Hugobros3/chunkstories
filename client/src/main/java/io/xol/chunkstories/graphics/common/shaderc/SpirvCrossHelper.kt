package io.xol.chunkstories.graphics.common.shaderc

import graphics.scenery.spirvcrossj.*
import io.xol.chunkstories.api.graphics.ShaderStage
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The API for that is really nasty and fugly so I'll contain it in this file */
object SpirvCrossHelper {
    val logger = LoggerFactory.getLogger("client.shaderc")

    init {
        Loader.loadNatives()
    }

    internal data class ProgramStage(val code: String, val stage: ShaderStage) {
        lateinit var tShader: TShader

        override fun toString(): String = "stage ${stage.extension}"
    }

    val ShaderStage.spirvStageInt: Int
        get() = when (this) {
            ShaderStage.VERTEX -> EShLanguage.EShLangVertex
            ShaderStage.GEOMETRY -> EShLanguage.EShLangGeometry
            ShaderStage.FRAGMENT -> EShLanguage.EShLangFragment
        }

    val ShaderStage.extension: String
        get() = when (this) {
            ShaderStage.VERTEX -> ".vert"
            ShaderStage.GEOMETRY -> ".geom"
            ShaderStage.FRAGMENT -> ".frag"
        }

    fun translateGLSLDialect(factory: ShaderFactory, dialect: ShaderFactory.GLSLDialect, stagesCode: Map<ShaderStage, String>): TranspiledGLSLProgram? {
        libspirvcrossj.initializeProcess()
        val ressources = libspirvcrossj.getDefaultTBuiltInResource()

        val program = TProgram()

        val stages = stagesCode.map { (stage, code) ->
            val codeWithIncludedStructs = ShaderWithResolvedIncludeStructs(factory, code)

            val codeWithInlinedStructs = factory.inlineStructsUsedAsUniformTypes(codeWithIncludedStructs)

            ProgramStage(codeWithInlinedStructs, stage)
        }

        for (stage in stages) {
            stage.tShader = TShader(stage.stage.spirvStageInt)

            stage.tShader.setStrings(arrayOf(stage.code), 1)
            stage.tShader.setAutoMapBindings(true)
            stage.tShader.setAutoMapLocations(true)

            var messages = EShMessages.EShMsgDefault
            messages = messages or EShMessages.EShMsgVulkanRules
            messages = messages or EShMessages.EShMsgSpvRules

            val parse = stage.tShader.parse(ressources, 450, false, messages)
            if (parse) logger.debug("parse OK") else {
                logger.warn(stage.tShader.infoLog)
                logger.warn(stage.tShader.infoDebugLog)
            }

            program.addShader(stage.tShader)
        }

        val link = program.link(EShMessages.EShMsgDefault)
        if (link) logger.debug("link OK") else logger.warn("link failed")

        val ioMap = program.mapIO()
        if (ioMap) logger.debug("io mapping OK") else logger.warn("io map failed")

        if (!link || !ioMap) {
            logger.warn(program.infoLog)
            logger.warn(program.infoDebugLog)

            return null
        }

        //program.buildReflection()

        fun ProgramStage.translateToGLSLDialect(): String {
            val intermediate = program.getIntermediate(this.stage.spirvStageInt)
            val intVec = IntVec()
            libspirvcrossj.glslangToSpv(intermediate, intVec)

            logger.debug("intermediary: ${intVec.size()} spirv bytes generated")

            val compiler = CompilerGLSL(intVec)
            val options = CompilerGLSL.Options()

            when (dialect) {
                ShaderFactory.GLSLDialect.OPENGL4 -> {
                    options.version = 400L
                    options.vulkanSemantics = false
                    options.enable420packExtension = false
                }
                ShaderFactory.GLSLDialect.VULKAN -> {
                    options.version = 450L
                    options.vulkanSemantics = true
                }
            }

            compiler.options = options

            val r = compiler.shaderResources

            val buffers = r.uniformBuffers
            for (i in 0 until buffers.size().toInt()) {
                val buffer = buffers.get(i)
                println("Found uniform block : name= ${buffer.name}")

                var set = compiler.getDecoration(buffer.id, Decoration.DecorationDescriptorSet)
                println("Descriptor set: $set")

                set = (Math.random() * 5).toLong()
                compiler.setDecoration(buffer.id, Decoration.DecorationDescriptorSet, set)

                assert(compiler.getDecoration(buffer.id, Decoration.DecorationDescriptorSet) == set)

                println("set set to $set")
            }

            val translatedGLSL = compiler.compile()

            return translatedGLSL
        }

        /*val vertexShader = stages.find { it.ext() == ".vert" }!!.translateToGLSLDialect()
        val geometryShader = stages.find { it.ext() == ".geom" }?.translateToGLSLDialect()
        val fragmentShader = stages.find { it.ext() == ".frag" }!!.translateToGLSLDialect()*/

        /* logger.debug("#uniforms blocks : ${program.numLiveUniformBlocks} loose : ${program.numLiveUniformVariables}")
         for(i in 0 until program.numLiveUniformBlocks) {
             val name = program.getUniformBlockName(i)
             val ttype = program.getUniformBlockTType(i)
             val binding = program.getUniformBlockBinding(i)
             val index = program.getUniformBlockIndex(i)
             println("uniform $i block :$name binding=$binding index=$index")
         }

         for(i in 0 until program.numLiveUniformVariables) {
             val name = program.getUniformName(i)
             val type = program.getUniformType(i)
             val ttype = program.getUniformTType(i)
             println("uniform $i :$name type=$type")
         }

         for(i in 0 until program.numLiveAttributes) {
             val name = program.getAttributeName(i)
             val type = program.getAttributeType(i)
             val ttype = program.getAttributeTType(i)

             println("attribute $i: $name $type")
             //println("GL_SAMPLER_2D=$GL_SAMPLER_2D")
         }*/

        //program.dumpReflection()

        libspirvcrossj.finalizeProcess()

        return TranspiledGLSLProgram(dialect, mapOf(*(stages.map { Pair(it.stage, it.translateToGLSLDialect()) }).toTypedArray()))
    }

    data class TranspiledGLSLProgram(val dialect: ShaderFactory.GLSLDialect, val stages: Map<ShaderStage, String>) {

    }

    fun generateSpirV(transpiledGLSL: TranspiledGLSLProgram): GeneratedSpirV? {
        libspirvcrossj.initializeProcess()
        val ressources = libspirvcrossj.getDefaultTBuiltInResource()

        val program = TProgram()

        val stages = transpiledGLSL.stages.map { (stage, code) -> ProgramStage(code, stage) }

        for (stage in stages) {
            stage.tShader = TShader(stage.stage.spirvStageInt)

            stage.tShader.setStrings(arrayOf(stage.code), 1)
            stage.tShader.setAutoMapBindings(true)
            stage.tShader.setAutoMapLocations(true)

            var messages = EShMessages.EShMsgDefault
            messages = messages or EShMessages.EShMsgVulkanRules
            messages = messages or EShMessages.EShMsgSpvRules

            val parse = stage.tShader.parse(ressources, 450, false, messages)
            if (parse) logger.debug("parse OK") else {
                logger.warn(stage.tShader.infoLog)
                logger.warn(stage.tShader.infoDebugLog)
            }

            program.addShader(stage.tShader)
        }

        val link = program.link(EShMessages.EShMsgDefault)
        if (link) logger.debug("link OK") else logger.warn("link failed")

        val ioMap = program.mapIO()
        if (ioMap) logger.debug("io mapping OK") else logger.warn("io map failed")

        if (!link || !ioMap) {
            logger.warn(program.infoLog)
            logger.warn(program.infoDebugLog)

            return null
        }

        fun ProgramStage.generateSpirV(): ByteBuffer {
            val intermediate = program.getIntermediate(this.stage.spirvStageInt)
            val intVec = IntVec()
            libspirvcrossj.glslangToSpv(intermediate, intVec)
            logger.debug("${intVec.size()} spirv bytes generated")

            return intVec.byteBuffer()
        }

        libspirvcrossj.finalizeProcess()

        return GeneratedSpirV(transpiledGLSL, mapOf(*stages.map { Pair(it.stage, it.generateSpirV()) }.toTypedArray()))
    }

    /** the generated spirv the engine can ingest for that shader program */
    data class GeneratedSpirV(val source: TranspiledGLSLProgram, val stages: Map<ShaderStage, ByteBuffer>) {

    }
}

private fun IntVec.byteBuffer(): ByteBuffer {
    val size = this.size().toInt()
    val bytes = ByteBuffer.allocateDirect(size * 4)
    //println(bytes.order())
    bytes.order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until size) {
        //val ri = i * 4

        val wth = this.get(i)
        //if(wth > Int.MAX_VALUE)
        //   println("rip")

        val wth2 = wth.toInt()
        //println("$i ${ri.hex()} $wth ${wth2}")

        bytes.putInt(wth2)
    }

    bytes.flip()
    return bytes
}

private fun ByteBuffer.bytes(): ByteArray {
    val bytes2 = ByteArray(this.limit())
    this.get(bytes2)
    return bytes2
}

public fun Int.hex(): String {
    var lol = ""
    var t = this

    for (nibble in 0..31 step 4) {
        val r = t and 0xF
        lol = hexs[r] + lol
        t = t shr 4
    }

    return lol
}

val hexs = "0123456789ABCDEF".toCharArray()