package xyz.chunkstories.graphics.common.shaders

import graphics.scenery.spirvcrossj.*
import org.lwjgl.opengl.GL20.GL_SAMPLER_2D
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.api.graphics.structs.UpdateFrequency
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass

/** The API for that is really nasty and fugly so I'll contain it in this file */
object SpirvCrossHelper {
    val logger = LoggerFactory.getLogger("client.shaders")

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

    //TODO actually nuke this and translate glsl manually, it'll be less painfull than dealing with this mess
    /*fun translateGLSLDialect(factory: ShaderCompiler, dialect: GLSLDialect, shaderCodePerStage: Map<ShaderStage, String>, content: Content?, shadersAssetBaseDir: String?): GLSLProgram {
        libspirvcrossj.initializeProcess()
        val ressources = libspirvcrossj.getDefaultTBuiltInResource()

        val program = TProgram()

        /** Vertex inputs that need a binding and a location decoration */
        val vertexInputsToProcess = mutableListOf<VertexInputDeclaration>()

        /** ubos that need a set/location decoration */
        val ubosToProcess = mutableListOf<Pair<String, InterfaceBlockGLSLMapping>>()

        val stages = shaderCodePerStage.map { (stage, code) ->
            val preprocessed = PreprocessedShaderStage(factory, code, stage, content, shadersAssetBaseDir)

            ubosToProcess.addAll(preprocessed.uniformBlocks)

            if (stage == ShaderStage.VERTEX) {
                vertexInputsToProcess.addAll(preprocessed.vertexInputs)
            }

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
            if (!parse) {
                logger.warn(stage.tShader.infoLog)
                logger.warn(stage.tShader.infoDebugLog)
            }

            program.addShader(stage.tShader)
        }

        val link = program.link(EShMessages.EShMsgDefault)
        if (!link) logger.warn("link failed")

        val ioMap = program.mapIO()
        if (!ioMap) logger.warn("io map failed")

        if (!link || !ioMap) {
            logger.warn(program.infoLog)
            logger.warn(program.infoDebugLog)

            throw Exception("Failed to link or map stages of the shader program")
        }

        val bucketsCount = when (dialect) {
            GLSLDialect.OPENGL4 -> 1 // ONE bucket, because descriptor sets aren't a thing in OpenGL

            /**
             * Bucket0: Virtual texturing descriptors
             * Bucket1: Static texturing descriptors ( declared samplers in the shader, you feed them manually )
             * Bucket2 .. BucketN+2: Descriptors for UBOs with frequency N
             */
            GLSLDialect.VULKAN -> UniformUpdateFrequency.values().size + 2
        }

        val resourcesBuckets = Array<MutableList<GLSLResource>>(bucketsCount) { mutableListOf() }

        program.buildReflection()

        libspirvcrossj.finalizeProcess()

        // TODO this is a hack because I can't use reflection to get info on the sampler itself
        val uniformTypeMap = mutableMapOf<String, Pair<Int, Int>>()
        for (index in 0 until program.numLiveUniformVariables) {
            uniformTypeMap[program.getUniformName(index)] = Pair(program.getUniformType(index), program.getUniformArraySize(index))
        }

        var vertexInputLocation = 0
        val vertexInputs = vertexInputsToProcess.map { input ->
            GLSLVertexInput(input.name, input.type, vertexInputLocation++)
        }

        fun ProgramStage.analyzeAndDecorateStageResources(): CompilerGLSL {
            val intermediate = program.getIntermediate(this.stage.spirvStageInt)
            val intVec = IntVec()
            libspirvcrossj.glslangToSpv(intermediate, intVec)

            //logger.debug("intermediary: ${intVec.size()} spirv bytes generated")

            val compiler = CompilerGLSL(intVec)
            val options = CompilerGLSL.Options()

            when (dialect) {
                ShaderCompiler.GLSLDialect.OPENGL4 -> {
                    options.version = 400L
                    options.vulkanSemantics = false
                    options.enable420packExtension = false
                }
                ShaderCompiler.GLSLDialect.VULKAN -> {
                    options.version = 450L
                    options.vulkanSemantics = true
                }
            }
            compiler.options = options

            fun KClass<InterfaceBlock>.updateFrequency(): UniformUpdateFrequency =
                    this.annotations.filterIsInstance<UpdateFrequency>().firstOrNull()?.frequency ?: UniformUpdateFrequency.ONCE_PER_FRAME

            val uniformBufferBlocks = compiler.shaderResources.uniformBuffers
            for (i in 0 until uniformBufferBlocks.size().toInt()) {
                val uniformBufferBlock = uniformBufferBlocks[i]

                var uniformBlockName = uniformBufferBlock.name

                /** Go over the uniform blocks and assign those a set & id */

                /** Go over the uniform blocks and assign those a set & id */

                /** Go over the uniform blocks and assign those a set & id */

                /** Go over the uniform blocks and assign those a set & id */
                if (uniformBlockName.startsWith("_inlined")) {

                    uniformBlockName = uniformBlockName.substring(uniformBlockName.indexOf('_') + 1)
                    uniformBlockName = uniformBlockName.substring(uniformBlockName.indexOf('_') + 1)

                    //println("Found uniform block : name= $uniformBlockName")

                    val inlinedUBO = ubosToProcess.find { it.first == uniformBlockName }
                            ?: throw Exception("UBO name starts with _inlined but we seemingly didn't create it ... Quoi la baise ! ")

                    val updateFrequency = inlinedUBO.second.klass.updateFrequency()

                    val descriptorSet = when (dialect) {
                        ShaderCompiler.GLSLDialect.OPENGL4 -> 0
                        ShaderCompiler.GLSLDialect.VULKAN -> updateFrequency.ordinal + 2
                    }

                    var uboResource = resourcesBuckets[descriptorSet].find { it is ShaderCompiler.GLSLUniformBlock && it.name == uniformBlockName && it.mapper == inlinedUBO.second }
                    if (uboResource == null) {
                        // Create and add the new resource to the corresponding bucket
                        uboResource = ShaderCompiler.GLSLUniformBlock(uniformBlockName, descriptorSet, resourcesBuckets[descriptorSet].size, inlinedUBO.second)
                        resourcesBuckets[descriptorSet].add(uboResource)
                    }

                    val binding = uboResource.binding

                    // Set the descriptor set decoration (important!)
                    compiler.setDecoration(uniformBufferBlock.id, Decoration.DecorationDescriptorSet, descriptorSet.toLong())
                    compiler.setDecoration(uniformBufferBlock.id, Decoration.DecorationBinding, binding.toLong())

                    // Check we did set the Descriptor Set
                    assert(compiler.getDecoration(uniformBufferBlock.id, Decoration.DecorationDescriptorSet) == descriptorSet.toLong())

                    println("Bound UBO $uniformBlockName to ($descriptorSet, $binding)")
                } else {
                    throw Exception("We require all uniform blocks to be typed with a #included struct.")
                }
            }

            val samplers = compiler.shaderResources.sampledImages
            for (i in 0 until samplers.size().toInt()) {
                val sampler = samplers[i]

                val samplerName = sampler.name

                //TODO Don't use this hack as soon as we can get the sampler type properly using
                //TODO sampler.typeId / sampler.baseTypeId
                //TODO https://github.com/scenerygraphics/spirvcrossj/issues/10

                val hack = uniformTypeMap[samplerName] ?: Pair(-1, 1)
                val samplerType = hack.first
                val samplerArraySize = hack.second

                // Set 0 is reserved for virtual textures, Set 1 is for static textures (accross all the draw)
                val descriptorSet = if (samplerName == "virtualTextures") 0 else 1
                val binding = resourcesBuckets[descriptorSet].size

                resourcesBuckets[descriptorSet].add(when (samplerType) {
                    GL_SAMPLER_2D -> ShaderCompiler.GLSLUniformSampler2D(samplerName, descriptorSet, binding, samplerArraySize)
                    else -> throw Exception("Unmapped/unhandled sampler type: $samplerType")//ShaderFactory.GLSLUnusedUniform(samplerName, descriptorSet, binding)
                })

                compiler.setDecoration(sampler.id, Decoration.DecorationDescriptorSet, descriptorSet.toLong())
                compiler.setDecoration(sampler.id, Decoration.DecorationBinding, binding.toLong())

                println("Bound Sampler $samplerName $samplerType to ($descriptorSet, $binding)")
            }

            // Modify the spirv to add the location decorations based on the locations we assigned earlier
            if (stage == ShaderStage.VERTEX) {
                val declaredVertexInputs = compiler.shaderResources.stageInputs
                for (i in 0 until declaredVertexInputs.size().toInt()) {
                    val spirvVertexInput = declaredVertexInputs[i]

                    vertexInputs.find { it.name == spirvVertexInput.name }?.let { vertexInput ->
                        compiler.setDecoration(spirvVertexInput.id, Decoration.DecorationLocation, vertexInput.location.toLong())
                        println("Vertex input ${spirvVertexInput.name} was assigned location ${vertexInput.location}")
                    }
                }
            }

            /*if (stage == ShaderStage.FRAGMENT) {
                val stageInputs = compiler.shaderResources.stageInputs

                for (i in 0 until stageInputs.size().toInt()) {
                    val stageInput = stageInputs[i]

                    println(stageInput.name + "st" + compiler.getType(stageInput.baseTypeId).basetype)
                    if (stageInput.name.equals("textureId")) {
                        //compiler.setDecoration(stageInput.id, 5300, 1)
                    }
                }
            }*/

            compiler.addHeaderLine("// Autogenerated GLSL code")
            return compiler
        }

        val partiallyDecoratedShaderStages = stages.map { it.analyzeAndDecorateStageResources() }
        val resources = resourcesBuckets.toList().merge().toMutableList()

        when (factory) {
            is VulkanShaderFactory -> {
                /*val virtualTexturingSlots = with(VirtualTexturingHelper) { factory.backend.getNumberOfSlotsForVirtualTexturing(resources) }

                partiallyDecoratedShaderStages.forEach {
                    it.addHeaderLine("layout(set=0, location=0) uniform sampler2D virtualTextures[${virtualTexturingSlots}];")
                    //TODO when that api works maybe ? it.shaderResources.sampledImages.pushBack(CombinedImageSampler())
                }

                val descriptorSet = 0
                val binding = resourcesBuckets[descriptorSet].size

                resources.add(ShaderFactory.GLSLUniformSampler2D("virtualTextures", descriptorSet, binding, virtualTexturingSlots))*/

                val fragmentStageIndex = stages.indexOf(stages.find { it.stage == ShaderStage.FRAGMENT })
                partiallyDecoratedShaderStages[fragmentStageIndex].addHeaderLine("#extension GL_EXT_nonuniform_qualifier : require")
            }
            //TODO OpenGL 4

            else -> partiallyDecoratedShaderStages.forEach {
                //it.addHeaderLine("// I guessed because this isn't actually linked to any concrete factory")
                //it.addHeaderLine("layout(set=0, location=0) uniform sampler2D virtualTextures[32];")

                //TODO when that api works maybe ? it.shaderResources.sampledImages.pushBack(CombinedImageSampler())
            }
        }

        val sources = mapOf(*(partiallyDecoratedShaderStages.mapIndexed { index, compiler ->
            var compiledSource = compiler.compile()

            if (dialect == ShaderCompiler.GLSLDialect.VULKAN) {
                val backend = (factory as? VulkanShaderFactory)?.backend
                if (backend != null) {
                    if (backend.enableDivergingUniformSamplerIndexing && backend.physicalDevice.canDoNonUniformSamplerIndexing) {

                        // Like, I could do this stuff cleanly using reflection and put the correct decoration in the spirvcode...
                        // But this works and stops me from wasting my time fighting the spirvcross swig nonsense
                        compiledSource = compiledSource.replace(Regex("virtualTextures\\[(([a-z]|[A-Z]).*)\\]")) {
                            "virtualTextures[nonuniformEXT(${it.groupValues[1]})]"
                        }
                    }
                } else {
                    logger.warn("Compiling to Vulkan semantics but this isn't a VulkanShaderFactory; Not annotating virtual texturing calls !")
                }
            }

            Pair(stages[index].stage, compiledSource)
        }).toTypedArray())

        return GLSLProgram(sources, vertexInputs, resources)
    }*/

    fun generateSpirV(transpiledGLSL: GLSLProgram): GeneratedSpirV {
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
            if (!parse) {
                logger.warn(stage.tShader.infoLog)
                logger.warn(stage.tShader.infoDebugLog)
            }

            program.addShader(stage.tShader)
        }

        val link = program.link(EShMessages.EShMsgDefault)
        val ioMap = program.mapIO()

        if (!link || !ioMap) {
            logger.warn(program.infoLog)
            logger.warn(program.infoDebugLog)

            throw Exception("Failed to link/map autogenerated translated code... this sucks now")
        }

        fun ProgramStage.generateSpirV(): ByteBuffer {
            val intermediate = program.getIntermediate(this.stage.spirvStageInt)
            val intVec = IntVec()
            libspirvcrossj.glslangToSpv(intermediate, intVec)
            logger.debug("${intVec.size()} spirv bytes generated")

            return intVec.byteBuffer()
        }

        libspirvcrossj.finalizeProcess()

        return GeneratedSpirV(transpiledGLSL, stages.associateBy({ it.stage }, { it.generateSpirV() }))
    }

    /** the generated spirv the engine can ingest for that shader program */
    data class GeneratedSpirV(val source: GLSLProgram, val stages: Map<ShaderStage, ByteBuffer>) {

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

//TODO move
private fun ByteBuffer.bytes(): ByteArray {
    val bytes2 = ByteArray(this.limit())
    this.get(bytes2)
    return bytes2
}

//TODO move
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

//TODO move
val hexs = "0123456789ABCDEF".toCharArray()