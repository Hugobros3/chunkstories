package io.xol.chunkstories.graphics.vulkan.shaderc

import ch.qos.logback.classic.Level
import graphics.scenery.spirvcrossj.*
import java.io.File
import graphics.scenery.spirvcrossj.EShMessages
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log

/** The API for that is really nasty and fugly so I'll contain it in this file */
object SpirvCrossHelper {
    val logger = LoggerFactory.getLogger("client.shaderc")

    init {
        Loader.loadNatives()
    }

    internal data class ShaderStage(val code: String, val type: Int) {
        fun ext() = when(type) {
            EShLanguage.EShLangVertex -> ".vert"
            EShLanguage.EShLangFragment -> ".frag"
            EShLanguage.EShLangGeometry -> ".geom"
            else -> throw Exception("Unhandled stage $type")
        }

        override fun toString(): String = "stage ${ext()}"
    }

    fun generateSpirV(vertexShaderCode: String, geometryShaderCode: String? = null, fragmentShaderCode: String) : GeneratedSpirV? {
        libspirvcrossj.initializeProcess()
        val ressources = libspirvcrossj.getDefaultTBuiltInResource()

        val program = TProgram()

        val stages = mutableListOf(ShaderStage(vertexShaderCode, EShLanguage.EShLangVertex), ShaderStage(fragmentShaderCode, EShLanguage.EShLangFragment))

        for(stage in stages) {
            val shader = TShader(stage.type)

            shader.setStrings(arrayOf(stage.code), 1)
            shader.setAutoMapBindings(true)
            shader.setAutoMapLocations(true)

            var messages = EShMessages.EShMsgDefault
            messages = messages or EShMessages.EShMsgVulkanRules
            messages = messages or EShMessages.EShMsgSpvRules

            val parse = shader.parse(ressources, 450, false, messages)
            if (parse) logger.debug("parse OK") else {
                logger.warn(shader.infoLog)
                logger.warn(shader.infoDebugLog)
            }

            program.addShader(shader)
        }

        val link = program.link(EShMessages.EShMsgDefault)
        if(link) logger.debug("link OK") else logger.warn("link failed")

        val ioMap = program.mapIO()
        if(ioMap) logger.debug("io mapping OK") else logger.warn("io map failed")

        if(!link || !ioMap) {
            logger.warn(program.infoLog)
            logger.warn(program.infoDebugLog)

            return null
        }

        for(stage in stages) {
            val intermediate = program.getIntermediate(stage.type)

            val intVec = IntVec()
            libspirvcrossj.glslangToSpv(intermediate, intVec)

            logger.debug("${intVec.size()} spirv bytes generated")

            val file = createTempFile(suffix = "${stage.ext()}.spv")
            file.writeBytes(intVec.byteBuffer().bytes())

            logger.debug("wrote $file")
        }

        libspirvcrossj.finalizeProcess()

        return GeneratedSpirV()
    }

    /** the generated spirv the engine can ingest for that shader program */
    class GeneratedSpirV() {

    }
}

private fun IntVec.byteBuffer(): ByteBuffer {
    val size = this.size().toInt()
    val bytes = ByteBuffer.allocate(size * 4)
    //println(bytes.order())
    bytes.order(ByteOrder.LITTLE_ENDIAN)

    for(i in 0 until size) {
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

private fun Int.hex(): String {
    var lol = ""
    var t = this

    for(nibble in 0..31 step 4) {
        val r = t and 0xF
        lol = hexs[r] + lol
        t = t shr 4
    }

    return lol
}

val hexs = "0123456789ABCDEF".toCharArray()