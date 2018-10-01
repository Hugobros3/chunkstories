package io.xol.chunkstories.graphics.vulkan.shaderc

import graphics.scenery.spirvcrossj.*
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The API for that is really nasty and fugly so I'll contain it in this file */
object SpirvCrossHelper {
    val logger = LoggerFactory.getLogger("client.shaderc")

    init {
        Loader.loadNatives()
    }

    fun loadProgram(basePath: String): GeneratedSpirV {
        val vertexShader = javaClass.getResource("$basePath.vert").readText()
        val fragmentShader = javaClass.getResource("$basePath.frag").readText()

        return generateSpirV(vertexShader, null, fragmentShader) ?: throw Exception("Failed to load program $basePath")
    }

    internal data class ShaderStage(val code: String, val type: Int) {
        lateinit var shader: TShader

        fun ext() = when (type) {
            EShLanguage.EShLangVertex -> ".vert"
            EShLanguage.EShLangGeometry -> ".geom"
            EShLanguage.EShLangFragment -> ".frag"
            else -> throw Exception("Unhandled stage $type")
        }

        override fun toString(): String = "stage ${ext()}"
    }

    fun generateSpirV(vertexShaderCode: String, geometryShaderCode: String? = null, fragmentShaderCode: String): GeneratedSpirV? {
        libspirvcrossj.initializeProcess()
        val ressources = libspirvcrossj.getDefaultTBuiltInResource()

        val program = TProgram()

        val stages = mutableListOf(ShaderStage(vertexShaderCode, EShLanguage.EShLangVertex), ShaderStage(fragmentShaderCode, EShLanguage.EShLangFragment))

        if (geometryShaderCode != null) {
            stages.add(ShaderStage(geometryShaderCode, EShLanguage.EShLangGeometry))
        }

        for (stage in stages) {
            stage.shader = TShader(stage.type)

            stage.shader.setStrings(arrayOf(stage.code), 1)
            stage.shader.setAutoMapBindings(true)
            stage.shader.setAutoMapLocations(true)

            var messages = EShMessages.EShMsgDefault
            messages = messages or EShMessages.EShMsgVulkanRules
            messages = messages or EShMessages.EShMsgSpvRules

            val parse = stage.shader.parse(ressources, 450, false, messages)
            if (parse) logger.debug("parse OK") else {
                logger.warn(stage.shader.infoLog)
                logger.warn(stage.shader.infoDebugLog)
            }

            program.addShader(stage.shader)
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

        program.buildReflection()

        fun ShaderStage.generateSpirV(): ByteBuffer {
            val intermediate = program.getIntermediate(this.type)
            val intVec = IntVec()
            libspirvcrossj.glslangToSpv(intermediate, intVec)
            logger.debug("${intVec.size()} spirv bytes generated")
            return intVec.byteBuffer()
        }

        val vertexShaderSpirV = stages.find { it.ext() == ".vert" }!!.generateSpirV()
        val geometryShaderSpirV = stages.find { it.ext() == ".geom" }?.generateSpirV()
        val fragmentShaderSpirV = stages.find { it.ext() == ".frag" }!!.generateSpirV()

        /*
        for(stage in stages) {
            val intermediate = program.getIntermediate(stage.type)

            val intVec = IntVec()
            libspirvcrossj.glslangToSpv(intermediate, intVec)

            logger.debug("${intVec.size()} spirv bytes generated")

            val file = createTempFile(suffix = "${stage.ext()}.spv")
            file.writeBytes(intVec.byteBuffer().bytes())

            logger.debug("wrote $file")
        }*/

        /*logger.debug("#uniforms blocks : ${program.numLiveUniformBlocks} loose : ${program.numLiveUniformVariables}")
        for(i in 0 until program.numLiveUniformBlocks) {
            val name = program.getUniformBlockName(i)
            val ttype = program.getUniformBlockTType(i)
            println("uniform $i block :$name ttype=$ttype")
        }

        for(i in 0 until program.numLiveUniformVariables) {
            val name = program.getUniformName(i)
            val type = program.getUniformType(i)
            val ttype = program.getUniformTType(i)
            println("uniform $i :$name type=$type $ttype")
        }

        for(i in 0 until program.numLiveAttributes) {
            val name = program.getAttributeName(i)
            val type = program.getAttributeType(i)
            val ttype = program.getAttributeTType(i)

            println("attribute $i: $name $type $ttype")
            println("GL_SAMPLER_2D=$GL_SAMPLER_2D")
        }*/

        program.dumpReflection()

        libspirvcrossj.finalizeProcess()

        return GeneratedSpirV(vertexShaderSpirV, geometryShaderSpirV, fragmentShaderSpirV)
    }

    /** the generated spirv the engine can ingest for that shader program */
    data class GeneratedSpirV(val vertexShaderSpirV: ByteBuffer, val geometryShaderSpirV: ByteBuffer?, val fragmentShaderSpirV: ByteBuffer) {

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