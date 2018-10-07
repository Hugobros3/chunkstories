package io.xol.chunkstories.graphics.common.shaderc

import graphics.scenery.spirvcrossj.*
import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.api.graphics.structs.UniformUpdateFrequency
import io.xol.chunkstories.api.graphics.structs.UpdateFrequency
import org.lwjgl.opengl.GL20.GL_SAMPLER_2D
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE
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

    fun translateGLSLDialect(factory: ShaderFactory, dialect: ShaderFactory.GLSLDialect, stagesCode: Map<ShaderStage, String>): ShaderFactory.GLSLProgram {
        libspirvcrossj.initializeProcess()
        val ressources = libspirvcrossj.getDefaultTBuiltInResource()

        val program = TProgram()

        val unlocatedUBOs = mutableListOf<Pair<String, InterfaceBlockGLSLMapping>>()

        val stages = stagesCode.map { (stage, code) ->
            val preprocessed = PreprocessedProgram(factory, code)

            // Transform the UBO (inline them so glslang is happier), and use the occasion to get a list of declared UBOs
            val uniformBlocks = with(factory) {
                preprocessed.findAndInlineUBOs()
            }

            unlocatedUBOs.addAll(uniformBlocks)

            ProgramStage(preprocessed.transformedCode, stage)
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

            throw Exception("Failed to link/map program !")
        }

        val resourcesBuckets = when (dialect) {
            ShaderFactory.GLSLDialect.OPENGL4 -> arrayOf(mutableListOf()) // ONE bucket, because descriptor sets aren't a thing in OpenGL

            // As many buckets as we have different uniform update frequencies (those map directly to Descriptor Sets)
            ShaderFactory.GLSLDialect.VULKAN -> Array<MutableList<ShaderFactory.GLSLUniformResource>>(UniformUpdateFrequency.values().size) { mutableListOf() }
        }

        //logger.debug("Sorting unlocated uniforms into buckets: ")
        //val locatedUniforms = mutableListOf<VulkanShaderFactory.VulkanShaderProgramUniformBlock>()
        /*for(updateFrequency in UniformUpdateFrequency.values()) {
            logger.debug("Bucket $updateFrequency : ")
            val ubosWithThisFrequency = unlocatedUBOs.filter { (_, mapping) -> mapping.klass.updateFrequency == updateFrequency }

            var binding = 0
            for(ubo in ubosWithThisFrequency) {
                val set = updateFrequency.ordinal
                logger.debug("Assigning binding ($set, $binding) to ubo ${ubo.first}")
                locatedUniforms += VulkanShaderFactory.VulkanShaderProgramUniformBlock(ubo.first, set, binding++, ubo.second)
            }
        }*/

        program.buildReflection()

        // TODO this is a hack because I can't use reflection to get info on the sampler itself
        val uniformTypeMap = mutableMapOf<String, Pair<Int, Int>>()
        for(index in 0 until program.numLiveUniformVariables) {
            uniformTypeMap[program.getUniformName(index)] = Pair(program.getUniformType(index), program.getUniformArraySize(index))
        }

        println(uniformTypeMap)

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


            fun kotlin.reflect.KClass<io.xol.chunkstories.api.graphics.structs.InterfaceBlock>.updateFrequency(): UniformUpdateFrequency  =
                    this.annotations.filterIsInstance<UpdateFrequency>().firstOrNull()?.frequency ?: UniformUpdateFrequency.ONCE_PER_BATCH

            val uniformBufferBlocks = compiler.shaderResources.uniformBuffers
            for (i in 0 until uniformBufferBlocks.size().toInt()) {
                val uniformBufferBlock = uniformBufferBlocks[i]

                var uniformBlockName = uniformBufferBlock.name

                /** Go over the uniform blocks and assign those a set & id */
                if (uniformBlockName.startsWith("_inlined")) {

                    uniformBlockName = uniformBlockName.substring(uniformBlockName.indexOf('_') + 1)
                    uniformBlockName = uniformBlockName.substring(uniformBlockName.indexOf('_') + 1)

                    //println("Found uniform block : name= $uniformBlockName")

                    val inlinedUBO = unlocatedUBOs.find { it.first == uniformBlockName }
                            ?: throw Exception("UBO name starts with _inlined but we seemingly didn't create it ... Quoi la baise ! ")

                    val updateFrequency = inlinedUBO.second.klass.updateFrequency()

                    val descriptorSet = when(dialect) {
                        ShaderFactory.GLSLDialect.OPENGL4 -> 0
                        ShaderFactory.GLSLDialect.VULKAN -> updateFrequency.ordinal + 1
                    }
                    val binding = resourcesBuckets[descriptorSet].size

                    // Set the descriptor set decoration (important!)
                    compiler.setDecoration(uniformBufferBlock.id, Decoration.DecorationDescriptorSet, descriptorSet.toLong())
                    compiler.setDecoration(uniformBufferBlock.id, Decoration.DecorationBinding, binding.toLong())

                    // Check we did set the Descriptor Set
                    assert(compiler.getDecoration(uniformBufferBlock.id, Decoration.DecorationDescriptorSet) == descriptorSet.toLong())

                    // Add the new resource to the corresponding bucket
                    resourcesBuckets[descriptorSet].add(ShaderFactory.GLSLUniformBlock(uniformBlockName, descriptorSet, binding, inlinedUBO.second))

                    println("Bound UBO $uniformBlockName to ($descriptorSet, $binding)")
                } else {
                    throw Exception("We require all uniform blocks to be typed with a #included struct.")
                }
            }

            val samplers = compiler.shaderResources.sampledImages
            for(i in 0 until samplers.size().toInt()) {
                val sampler = samplers[i]

                val samplerName = sampler.name

                //TODO TODO TODO val samplerType = sampler.typeId
                val hack = uniformTypeMap[samplerName] ?: Pair(-1, 1)
                val samplerType = hack.first
                val samplerArraySize = hack.second

                val descriptorSet = 0
                val binding = resourcesBuckets[descriptorSet].size

                resourcesBuckets[descriptorSet].add(when(samplerType) {
                    GL_SAMPLER_2D -> ShaderFactory.GLSLUniformSampler2D(samplerName, descriptorSet, binding, samplerArraySize)
                    else -> ShaderFactory.GLSLUnusedUniform(samplerName, descriptorSet, binding)
                })

                compiler.setDecoration(sampler.id, Decoration.DecorationDescriptorSet, descriptorSet.toLong())
                compiler.setDecoration(sampler.id, Decoration.DecorationBinding, binding.toLong())

                println("Bound Sampler $samplerName $samplerType to ($descriptorSet, $binding)")
            }

            compiler.addHeaderLine("// Autogenerated GLSL code")

            return compiler.compile()
        }

        libspirvcrossj.finalizeProcess()

        val sources = mapOf(*(stages.map { Pair(it.stage, it.translateToGLSLDialect()) }).toTypedArray())
        val resources = resourcesBuckets.toList().merge()

        return ShaderFactory.GLSLProgram(sources, resources)
    }

    fun generateSpirV(transpiledGLSL: ShaderFactory.GLSLProgram): GeneratedSpirV? {
        libspirvcrossj.initializeProcess()
        val ressources = libspirvcrossj.getDefaultTBuiltInResource()

        val program = TProgram()

        val stages = transpiledGLSL.sourceCode.map { (stage, code) -> ProgramStage(code, stage) }

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
    data class GeneratedSpirV(val source: ShaderFactory.GLSLProgram, val stages: Map<ShaderStage, ByteBuffer>) {

    }
}

private fun <E> List<List<E>>.merge(): List<E> {
    val list = mutableListOf<E>()
    this.forEach { list.addAll(it) }
    return list
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