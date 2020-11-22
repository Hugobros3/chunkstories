package xyz.chunkstories.graphics.common.shaders.compiler

import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.GLSLGraphicsProgram
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.MaterialImage
import xyz.chunkstories.graphics.common.shaders.compiler.postprocessing.addVirtualTexturingHeader
import xyz.chunkstories.graphics.common.shaders.compiler.postprocessing.annotateForNonUniformAccess
import xyz.chunkstories.graphics.common.shaders.compiler.preprocessing.*
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.*
import kotlin.reflect.KClass

abstract class ShaderCompiler(val dialect: GLSLDialect) {
    abstract val classLoader: ClassLoader
    abstract val content: Content?

    abstract val newResourceLocationAssigner: () -> ResourceLocationAssigner
    abstract val spirv_13: Boolean

    /** We keep track of all the JVM classes we encounter so we don't have to do the mapping into GLSL structs every time */
    val jvmGlslMappings = mutableMapOf<KClass<InterfaceBlock>, GLSLType.JvmStruct>()

    init {
        //Loader.loadNatives()
        SpirvCrossHelper.initSpirvCross()
    }

    fun loadGLSLProgram(shaderName: String, compilationParameters: ShaderCompilationParameters = ShaderCompilationParameters()) : GLSLGraphicsProgram {
        var tries = 0
        while(true) {
            try {
                val shaderBaseDir = "shaders/$shaderName"

                val vertexShader = readShaderFile("$shaderBaseDir/$shaderName.vert")
                        ?: throw Exception("Vertex shader stage not found in either built-in resources or assets for shader: $shaderName")
                val fragmentShader = readShaderFile("$shaderBaseDir/$shaderName.frag")
                        ?: throw Exception("Fragment shader stage not found in either built-in resources or assets for shader: $shaderName")
                //TODO add optional geometry/tesselation stages here

                var stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

                //stages = stages.mapValues { (stage, shaderCode) -> removeVersionString(shaderCode) }

                // Process #include statements
                stages = stages.mapValues { (stage, shaderCode) -> processFileIncludes(shaderBaseDir, shaderCode) }
                stages = stages.mapValues { (stage, shaderCode) -> addDefines(shaderCode, compilationParameters.defines) }
                stages = stages.mapValues { (stage, shaderCode) -> runPrePreProcessor(shaderCode) }

                // Process #using struct statements
                // Find them and extract the required classes
                val jvmClassesUsed = stages.mapValues { (_, shaderCode) -> findUsedJvmClasses(shaderCode) }
                // Obtain/translate these class into the correct GLSL structs
                val jvmStructsUsed = jvmClassesUsed.mapValues { (_, it) -> it.map { getGlslStruct(it) } }
                // Add their declarations, in the right order.
                stages = stages.mapValues { (stage, shaderCode) -> addStructsDeclaration(shaderCode, jvmStructsUsed[stage]!!) }

                stages = stages.mapValues { (stage, shaderCode) -> inlineUniformStructs(shaderCode, jvmStructsUsed[stage]!!) }
                stages = stages.mapValues { (stage, shaderCode) -> inlinePerInstanceData(shaderCode, jvmStructsUsed[stage]!!) }

                stages = stages.mapValues { (stage, shaderCode) -> addVirtualTexturingHeader(shaderCode) }

                val materialBoundResources = mutableSetOf<String>()
                stages = stages.mapValues { (stage, shaderCode) -> findAndReplaceMaterialBoundResources(shaderCode, materialBoundResources) }

                val vertexInputs = analyseVertexShaderInputs(stages[ShaderStage.VERTEX]!!)
                val fragmentOutputs = analyseFragmentShaderOutputs(stages[ShaderStage.FRAGMENT]!!)

                stages = stages.toMutableMap()

                compilationParameters.outputs?.let {
                    (stages as MutableMap<ShaderStage, String>)[ShaderStage.FRAGMENT] = removeUnusedOutputs(stages[ShaderStage.FRAGMENT]!!, fragmentOutputs, it)
                }

                compilationParameters.inputs?.let {
                    (stages as MutableMap<ShaderStage, String>)[ShaderStage.VERTEX] = removeMissingInputs(stages[ShaderStage.VERTEX]!!, vertexInputs, it)
                }


                val intermediaryCompilationResults = buildIntermediaryStructure(stages, spirv_13)
                val (perInstanceDataInputs, resources) = createShaderResources(intermediaryCompilationResults, materialBoundResources)

                addDecorations(intermediaryCompilationResults, resources, perInstanceDataInputs)
                stages = toIntermediateGLSL(intermediaryCompilationResults)

                //if(this is VulkanShaderFactory && this.backend.enableDivergingUniformSamplerIndexing)
                if (dialect == GLSLDialect.VULKAN)
                    stages = stages.mapValues { (stage, shaderCode) -> annotateForNonUniformAccess(shaderCode) }

                //val perInstanceDataInputs = resources.filterIsInstance<GLSLShaderStorage>().mapNotNull { it.associatedInstanceData }
                return GLSLGraphicsProgram(shaderName, dialect, vertexInputs, fragmentOutputs, perInstanceDataInputs, resources, materialBoundResources.map { MaterialImage(it) }, stages)
            } catch(e: Exception) {
                tries++
                logger.error("Shader compilation failed! Retrying in 10s to allow dev to iterate ...")
                logger.error(e.message.toString())
                //if(e.message == "null")
                    e.printStackTrace()
                Thread.sleep(10_000)
            }
        }
    }

    fun readShaderFile(path: String): String? {
        val internalResource = javaClass.getResource("/$path")
        if (internalResource != null) {
            return internalResource.readText()
        }

        val asset = content?.getAsset(path)
        if(asset != null) {
            return asset.reader().readText()
        }

        return null
    }

    companion object {
        val logger = LoggerFactory.getLogger("shaderCompiler")
    }
}